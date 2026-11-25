const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('./database');
const authenticateToken = require('./middleware/auth');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
// Limite ampliado a 5mb para aceptar imagenes comprimidas en Base64 sin guardar archivos en disco
app.use(express.json({ limit: '5mb' }));

// Verificacion rapida de estado del servidor
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'API de perfil funcionando correctamente.' });
});

// 1. Registro de usuario
app.post('/api/auth/register', async (req, res) => {
  try {
    const { fullName, email, password } = req.body;

    if (!fullName || !email || !password) {
      return res.status(400).json({ message: 'Todos los campos son obligatorios.' });
    }

    const cleanName = fullName.trim();
    const cleanEmail = email.trim().toLowerCase();

    if (password.length < 6) {
      return res.status(400).json({ message: 'La contraseña debe tener al menos 6 caracteres.' });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const sql = `INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)`;
    db.run(sql, [cleanName, cleanEmail, passwordHash], function (err) {
      if (err) {
        if (err.message.includes('UNIQUE constraint failed')) {
          return res.status(409).json({ message: 'El correo electrónico ya está registrado.' });
        }
        return res.status(500).json({ message: 'Error interno al registrar usuario.' });
      }

      return res.status(201).json({
        message: 'Usuario registrado exitosamente.',
        user: {
          id: this.lastID,
          fullName: cleanName,
          email: cleanEmail
        }
      });
    });
  } catch (error) {
    return res.status(500).json({ message: 'Error inesperado en el servidor.' });
  }
});

// 2. Inicio de sesion (Login)
app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ message: 'Debe ingresar correo y contraseña.' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const sql = `SELECT id, full_name, email, password_hash, profile_image_base64 FROM users WHERE email = ?`;

  db.get(sql, [cleanEmail], async (err, user) => {
    if (err) {
      return res.status(500).json({ message: 'Error interno al consultar usuario.' });
    }
    if (!user) {
      return res.status(401).json({ message: 'Credenciales incorrectas.' });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ message: 'Credenciales incorrectas.' });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: '24h' }
    );

    return res.json({
      message: 'Inicio de sesión exitoso.',
      token,
      user: {
        id: user.id,
        fullName: user.full_name,
        email: user.email,
        profileImageBase64: user.profile_image_base64 || null
      }
    });
  });
});

// 3. Obtener perfil del usuario autenticado
app.get('/api/profile', authenticateToken, (req, res) => {
  const sql = `SELECT id, full_name, email, profile_image_base64, created_at FROM users WHERE id = ?`;

  db.get(sql, [req.user.id], (err, user) => {
    if (err) {
      return res.status(500).json({ message: 'Error interno al obtener perfil.' });
    }
    if (!user) {
      return res.status(404).json({ message: 'Usuario no encontrado.' });
    }

    return res.json({
      id: user.id,
      fullName: user.full_name,
      email: user.email,
      profileImageBase64: user.profile_image_base64 || null,
      createdAt: user.created_at
    });
  });
});

// 4. Actualizar imagen de perfil en Base64 dentro de la BD relacional
app.put('/api/profile/photo', authenticateToken, (req, res) => {
  const { imageBase64 } = req.body;

  if (!imageBase64 || typeof imageBase64 !== 'string') {
    return res.status(400).json({ message: 'Debe enviar una imagen válida en Base64.' });
  }

  const sql = `UPDATE users SET profile_image_base64 = ? WHERE id = ?`;
  db.run(sql, [imageBase64, req.user.id], function (err) {
    if (err) {
      return res.status(500).json({ message: 'Error interno al actualizar la foto.' });
    }
    if (this.changes === 0) {
      return res.status(404).json({ message: 'Usuario no encontrado.' });
    }

    return res.json({
      message: 'Foto de perfil actualizada correctamente.',
      profileImageBase64: imageBase64
    });
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Servidor backend escuchando en http://localhost:${PORT}`);
});