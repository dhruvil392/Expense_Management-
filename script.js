<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  // --- Constants and Configuration ---
const STORAGE_KEY = 'enterprise_expenses_data';
const BASE_CURRENCY_KEY = 'company_base_currency';
const APPROVAL_FLOW = ['Manager', 'Finance', 'Director']; // Hardcoded multi-level flow simulation
const DEFAULT_COMPANY_CURRENCY = 'USD';

// --- Data & Storage Utilities ---

function loadState() {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        return stored ? JSON.parse(stored) : { expenses: [], currencyRates: {}, companyCurrency: DEFAULT_COMPANY_CURRENCY };
    } catch (error) {
        console.error("Error loading state:", error);
        return { expenses: [], currencyRates: {}, companyCurrency: DEFAULT_COMPANY_CURRENCY };
    }
}

function saveExpenses(expenses, companyCurrency, currencyRates) {
    try {
        const state = { expenses, companyCurrency, currencyRates };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (error) {
        console.error("Error saving expenses:", error);
    }
}

// --- API Utilities (Asynchronous Operations) ---

/**
 * Fetches all world currencies for the dropdowns.
 * Uses a robust, public API.
 */
async function fetchCurrencies() {
    const URL = 'https://restcountries.com/v3.1/all?fields=name,currencies';
    try {
        const response = await fetch(URL);
        const countries = await response.json();
        const currencies = {};
        countries.forEach(country => {
            if (country.currencies) {
                // Get the currency code from the currencies object key
                const code = Object.keys(country.currencies)[0];
                const name = country.currencies[code].name;
                currencies[code] = name;
            }
        });
        return Object.keys(currencies).sort();
    } catch (error) {
        console.error("Could not fetch country currencies:", error);
        return [DEFAULT_COMPANY_CURRENCY]; // Fallback
    }
}

/**
 * Fetches currency conversion rates relative to the base currency.
 */
async function fetchExchangeRates(baseCurrency) {
    const URL = https://api.exchangerate-api.com/v4/latest/${baseCurrency};
    try {
        const response = await fetch(URL);
        const data = await response.json();
        return data.rates || {};
    } catch (error) {
        console.error("Could not fetch exchange rates:", error);
        return {};
    }
}

// --- Main Application Class ---

class ExpenseManager {
    constructor() {
        this.state = loadState();
        this.expenses = this.state.expenses;
        this.currencyRates = this.state.currencyRates;
        this.companyCurrency = this.state.companyCurrency;
        this.currentUser = null;
        this.initApp();
    }

    async initApp() {
        await this.populateCurrencyDropdowns();
        this.initEventListeners();
        this.renderTables(); // Initial render
    }

    initEventListeners() {
        document.getElementById('login-btn')?.addEventListener('click', () => this.login());
        document.getElementById('logout-btn')?.addEventListener('click', () => this.logout());
        document.getElementById('expense-form')?.addEventListener('submit', (e) => this.handleSubmitExpense(e));
        document.getElementById('dashboard')?.addEventListener('click', (e) => this.handleActionClick(e));
    }

    // --- Currency Helpers ---

    async populateCurrencyDropdowns() {
        const currencies = await fetchCurrencies();
        const baseSelect = document.getElementById('base-currency');
        const expenseSelect = document.getElementById('expense-currency');
        
        [baseSelect, expenseSelect].forEach(select => {
            select.innerHTML = '';
            currencies.forEach(code => {
                const option = document.createElement('option');
                option.value = code;
                option.textContent = code;
                select.appendChild(option);
            });
        });

        // Set the currently stored company currency
        if (baseSelect) baseSelect.value = this.companyCurrency;
    }

    async getExchangeRate(fromCurrency, toCurrency) {
        if (fromCurrency === toCurrency) return 1;

        // Check if rates for the company base are available
        if (this.currencyRates && this.currencyRates[fromCurrency] && this.currencyRates[toCurrency]) {
             // Rate is (To/Base) / (From/Base) = To/From
            return this.currencyRates[toCurrency] / this.currencyRates[fromCurrency];
        }

        // Fallback: Fetch new rates if necessary (slow, but robust)
        console.warn(Rates for ${this.companyCurrency} not found. Fetching new rates...);
        this.currencyRates = await fetchExchangeRates(fromCurrency);
        if (this.currencyRates[toCurrency]) {
            // Save updated rates
            this.saveAppState();
            return this.currencyRates[toCurrency];
        }

        console.error(Conversion rate from ${fromCurrency} to ${toCurrency} not found.);
        return 1; // Default to 1 if conversion fails
    }

    // --- Authentication and UI Handlers ---

    async login() {
        const usernameInput = document.getElementById('username');
        const roleSelect = document.getElementById('role');
        const baseCurrencySelect = document.getElementById('base-currency');
        
        const username = usernameInput.value.trim();
        const role = roleSelect.value;
        const newCompanyCurrency = baseCurrencySelect.value;

        if (!username || !newCompanyCurrency) {
            alert('Please enter username and select a base currency.');
            return;
        }

        this.currentUser = { username, role };
        
        // Update company currency and fetch rates if changed
        if (newCompanyCurrency !== this.companyCurrency) {
            this.companyCurrency = newCompanyCurrency;
            this.currencyRates = await fetchExchangeRates(this.companyCurrency);
            this.saveAppState();
        }

        document.getElementById('login-form').classList.replace('d-block', 'd-none');
        document.getElementById('dashboard').classList.replace('d-none', 'd-block');
        document.getElementById('welcome-message').textContent = Welcome, ${username} (${role})!;
        document.getElementById('company-currency-display').textContent = this.companyCurrency;


        this.showPanel(role);
        this.renderTables();
    }

    logout() {
        this.currentUser = null;
        document.getElementById('login-form').classList.replace('d-none', 'd-block');
        document.getElementById('dashboard').classList.replace('d-block', 'd-none');
        this.showPanel(null);
    }

    showPanel(role) {
        ['employee-panel', 'manager-panel', 'admin-panel'].forEach(id => document.getElementById(id).classList.add('d-none'));

        if (role === 'employee') document.getElementById('employee-panel').classList.remove('d-none');
        if (role === 'manager') document.getElementById('manager-panel').classList.remove('d-none');
        if (role === 'admin') document.getElementById('admin-panel').classList.remove('d-none');
    }
    
    // --- Expense Submission and Action Handlers ---

    async handleSubmitExpense(event) {
        event.preventDefault(); 
        
        const category = document.getElementById('expense-category').value;
        const amount = parseFloat(document.getElementById('expense-amount').value);
        const expenseCurrency = document.getElementById('expense-currency').value;
        const date = document.getElementById('expense-date').value;
        const desc = document.getElementById('expense-desc').value;

        if (!category || isNaN(amount) || amount <= 0 || !date) {
            alert('Please fill all required fields with valid data.');
            return;
        }
        
        // Multi-Currency Logic: Convert to base currency
        const rate = await this.getExchangeRate(expenseCurrency, this.companyCurrency);
        const baseAmount = (amount * rate).toFixed(2);
        
        const newExpense = {
            id: Date.now(),
            employee: this.currentUser.username,
            category: category.trim(),
            amount: amount.toFixed(2), 
            currency: expenseCurrency,
            baseAmount: baseAmount,
            date,
            desc: desc.trim(),
            status: 'Pending',
            // Multi-Level Approval Simulation
            approvers: APPROVAL_FLOW.slice(), 
            currentApproverRole: APPROVAL_FLOW[0] 
        };

        this.expenses.push(newExpense);
        this.saveAppState();
        document.getElementById('expense-form').reset(); 
        alert(Expense submitted successfully! Base currency amount: ${this.companyCurrency} ${baseAmount});
        this.renderTables();
    }

    handleActionClick(event) {
        const target = event.target;
        if (target.tagName !== 'BUTTON' || !target.dataset.id) return;

        const id = parseInt(target.dataset.id);
        const comment = prompt("Enter approval/rejection comment:");
        if (comment === null) return; // User cancelled

        if (target.dataset.action === 'approve') {
            this.updateApprovalStatus(id, 'Approved', comment);
        } else if (target.dataset.action === 'reject') {
            this.updateApprovalStatus(id, 'Rejected', comment);
        }
    }

    /**
     * Simulates the multi-level approval movement.
     */
    updateApprovalStatus(id, action, comment) {
        const exp = this.expenses.find(e => e.id === id);
        if (!exp) return;

        if (action === 'Rejected') {
            exp.status = Rejected by ${this.currentUser.role};
            exp.currentApproverRole = null; // Flow terminates
        } else if (action === 'Approved') {
            const currentRoleIndex = APPROVAL_FLOW.indexOf(exp.currentApproverRole);
            const nextRoleIndex = currentRoleIndex + 1;

            if (nextRoleIndex < exp.approvers.length) {
                // Move to next step
                exp.currentApproverRole = exp.approvers[nextRoleIndex];
                exp.status = Pending (${exp.currentApproverRole} Review);
            } else {
                // Final approval reached
                exp.status = 'Fully Approved';
                exp.currentApproverRole = null;
            }
        }
        
        // Log the approval action (for a real system)
        console.log(${action} by ${this.currentUser.role} on expense ${id}: ${comment});
        
        this.saveAppState();
        this.renderTables();
    }
    
    saveAppState() {
        saveExpenses(this.expenses, this.companyCurrency, this.currencyRates);
    }

    // --- Table Rendering ---

    createTableRow(e, type) {
        const tr = document.createElement('tr');
        const nextApprover = e.currentApproverRole || 'N/A';
        const formattedBaseAmount = ${this.companyCurrency} ${e.baseAmount};
        const formattedSubmittedAmount = ${e.currency} ${e.amount};
        
        let statusClass = 'bg-secondary';
        if (e.status.startsWith('Fully Approved')) statusClass = 'bg-success';
        if (e.status.startsWith('Rejected')) statusClass = 'bg-danger';

        const statusBadge = <span class="badge ${statusClass} p-2">${e.status}</span>;

        if (type === 'employee') {
            tr.innerHTML = `
                <td>${e.id}</td>
                <td>${e.category}</td>
                <td>${formattedSubmittedAmount}</td>
                <td>${formattedBaseAmount}</td>
                <td>${statusBadge}</td>
                <td>${nextApprover}</td>
            `;
        } else if (type === 'manager') {
            const actionButtons = nextApprover === this.currentUser.role ? `
                <button data-id="${e.id}" data-action="approve" class="btn btn-sm btn-success">Approve</button> 
                <button data-id="${e.id}" data-action="reject" class="btn btn-sm btn-danger">Reject</button>
            ` : <span class="badge bg-dark p-2">${e.status}</span>;
            
            tr.innerHTML = `
                <td>${e.id}</td>
                <td>${e.employee}</td>
                <td>${e.category}</td>
                <td>${formattedSubmittedAmount}</td>
                <td>${formattedBaseAmount}</td>
                <td>${e.date}</td>
                <td>${actionButtons}</td>
            `;
        } else if (type === 'admin') {
            tr.innerHTML = `
                <td>${e.id}</td>
                <td>${e.employee}</td>
                <td>${e.category}</td>
                <td>${formattedSubmittedAmount}</td>
                <td>${formattedBaseAmount}</td>
                <td>${statusBadge}</td>
                <td>${nextApprover}</td>
            `;
        }
        return tr;
    }

    renderTables() {
        const currentUsername = this.currentUser?.username;
        const currentRole = this.currentUser?.role;
        const allExpenses = this.expenses.slice().sort((a, b) => b.id - a.id); 

        // 1. Employee Table
        const empTbody = document.querySelector('#employee-expenses-table tbody');
        if (empTbody) {
            empTbody.innerHTML = '';
            allExpenses
                .filter(e => e.employee === currentUsername)
                .forEach(e => empTbody.appendChild(this.createTableRow(e, 'employee')));
        }

        // 2. Manager Table (Pending Approvals)
        const mgrTbody = document.querySelector('#manager-expenses-table tbody');
        if (mgrTbody) {
            mgrTbody.innerHTML = '';
            // Filter: Only show expenses where the current role is the next approver
            allExpenses
                .filter(e => e.currentApproverRole === currentRole)
                .forEach(e => mgrTbody.appendChild(this.createTableRow(e, 'manager')));
        }

        // 3. Admin Table (All Expenses)
        const adminTbody = document.querySelector('#admin-expenses-table tbody');
        if (adminTbody) {
            adminTbody.innerHTML = '';
            allExpenses
                .forEach(e => adminTbody.appendChild(this.createTableRow(e, 'admin')));
        }
    }
}

// Instantiate the application when the window loads
window.addEventListener('load', () => {
    new ExpenseManager();
});
