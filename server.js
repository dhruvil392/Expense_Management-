require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const Joi = require('joi');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 4000;

app.use(express.json());

// Multer setup for local file storage
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir);

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    cb(null, Date.now() + '-' + file.originalname.replace(/\s/g, '_'));
  }
});
const upload = multer({ storage });

// === Mongoose models ===
const userSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true, trim: true },
  password: { type: String, required: true },
  role: { type: String, enum: ['employee', 'manager', 'admin'], default: 'employee' }
});

userSchema.pre('save', async function(next) {
  if (!this.isModified('password')) return next();
  this.password = await bcrypt.hash(this.password, 10);
  next();
});

userSchema.methods.comparePassword = function(password) {
  return bcrypt.compare(password, this.password);
};

const User = mongoose.model('User', userSchema);

const expenseSchema = new mongoose.Schema({
  employee: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  category: { type: String, required: true },
  amount: { type: Number, required: true },
  currency: { type: String, required: true },
  baseAmount: { type: Number, required: true },
  date: { type: Date, required: true },
  desc: String,
  status: { type: String, enum: ['Pending', 'Approved', 'Rejected'], default: 'Pending' },
  currentApproverRole: { type: String, enum: ['manager', 'admin'], required: true },
  receiptPath: String,
}, { timestamps: true });

const Expense = mongoose.model('Expense', expenseSchema);

// === Middleware ===

// Auth middleware
const protect = async (req, res, next) => {
  let token;
  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer ')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }
  if (!token) return res.status(401).json({ message: 'Not authorized, no token' });

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select('-password');
    if (!user) return res.status(401).json({ message: 'User not found' });
    req.user = user;
    next();
  } catch {
    return res.status(401).json({ message: 'Token invalid or expired' });
  }
};

// Role middleware
const authorizeRoles = (...roles) => (req, res, next) => {
  if (!roles.includes(req.user.role)) return res.status(403).json({ message: 'Access denied' });
  next();
};

// Error handler
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ message: err.message || 'Server error' });
});

// === Routes ===

// Register
app.post('/api/auth/register', async (req, res, next) => {
  try {
    const schema = Joi.object({
      username: Joi.string().min(3).required(),
      password: Joi.string().min(6).required(),
      role: Joi.string().valid('employee', 'manager', 'admin').default('employee'),
    });
    const { username, password, role } = await schema.validateAsync(req.body);

    if (await User.findOne({ username })) {
      return res.status(400).json({ message: 'Username already taken' });
    }

    const user = new User({ username, password, role });
    await user.save();

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.status(201).json({ token, user: { id: user._id, username, role } });
  } catch (err) {
    next(err);
  }
});

// Login
app.post('/api/auth/login', async (req, res, next) => {
  try {
    const schema = Joi.object({
      username: Joi.string().required(),
      password: Joi.string().required(),
    });
    const { username, password } = await schema.validateAsync(req.body);

    const user = await User.findOne({ username });
    if (!user) return res.status(401).json({ message: 'Invalid credentials' });

    const isValid = await user.comparePassword(password);
    if (!isValid) return res.status(401).json({ message: 'Invalid credentials' });

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.json({ token, user: { id: user._id, username, role: user.role } });
  } catch (err) {
    next(err);
  }
});

// Create expense
app.post('/api/expenses', protect, upload.single('receipt'), async (req, res, next) => {
  try {
    const schema = Joi.object({
      category: Joi.string().required(),
      amount: Joi.number().positive().required(),
      currency: Joi.string().required(),
      baseAmount: Joi.number().positive().required(),
      date: Joi.date().required(),
      desc: Joi.string().allow(''),
      currentApproverRole: Joi.string().valid('manager', 'admin').required(),
    });

    const {
      category, amount, currency, baseAmount, date, desc, currentApproverRole
    } = await schema.validateAsync(req.body);

    const receiptPath = req.file ? req.file.path : null;

    const expense = new Expense({
      employee: req.user._id,
      category,
      amount,
      currency,
      baseAmount,
      date,
      desc,
      currentApproverRole,
      receiptPath,
      status: 'Pending',
    });

    await expense.save();
    res.status(201).json(expense);
  } catch (err) {
    next(err);
  }
});

// Get expenses (employees see their own, managers/admins see all)
app.get('/api/expenses', protect, async (req, res, next) => {
  try {
    let expenses;
    if (req.user.role === 'employee') {
      expenses = await Expense.find({ employee: req.user._id }).populate('employee', 'username role');
    } else {
      expenses = await Expense.find().populate('employee', 'username role');
    }
    res.json(expenses);
  } catch (err) {
    next(err);
  }
});

// Approve expense (only managers/admins)
app.put('/api/expenses/:id/approve', protect, authorizeRoles('manager', 'admin'), async (req, res, next) => {
  try {
    const expense = await Expense.findById(req.params.id);
    if (!expense) return res.status(404).json({ message: 'Expense not found' });
    if (expense.status !== 'Pending') return res.status(400).json({ message: 'Expense already processed' });

    expense.status = 'Approved';
    await expense.save();

    res.json({ message: 'Expense approved', expense });
  } catch (err) {
    next(err);
  }
});

// Reject expense (only managers/admins)
app.put('/api/expenses/:id/reject', protect, authorizeRoles('manager', 'admin'), async (req, res, next) => {
  try {
    const expense = await Expense.findById(req.params.id);
    if (!expense) return res.status(404).json({ message: 'Expense not found' });
    if (expense.status !== 'Pending') return res.status(400).json({ message: 'Expense already processed' });

    expense.status = 'Rejected';
    await expense.save();

    res.json({ message: 'Expense rejected', expense });
  } catch (err) {
    next(err);
  }
});

// Serve uploaded files statically
app.use('/uploads', express.static(uploadDir));

// Connect DB and start server
mongoose.connect(process.env.MONGO_URI, {
  useNewUrlParser: true, useUnifiedTopology: true
})
.then(() => {
  console.log('MongoDB connected');
  app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
})
.catch(err => {
  console.error('MongoDB connection error:', err);
});
