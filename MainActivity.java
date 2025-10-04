package com.example.myapplication;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    LinearLayout rootLayout;

    // --- In-memory data ---
    Company company;
    Map<String, User> users = new HashMap<>();
    User loggedInUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootLayout = findViewById(R.id.rootLayout);

        showRoleSelection();
    }

    // --------- UI Screens -------------

    void showRoleSelection() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Select Role to Login:");
        tv.setTextSize(22);
        rootLayout.addView(tv);

        Button adminBtn = new Button(this);
        adminBtn.setText("Admin");
        adminBtn.setOnClickListener(v -> adminLogin());
        rootLayout.addView(adminBtn);

        Button managerBtn = new Button(this);
        managerBtn.setText("Manager");
        managerBtn.setOnClickListener(v -> managerLogin());
        rootLayout.addView(managerBtn);

        Button employeeBtn = new Button(this);
        employeeBtn.setText("Employee");
        employeeBtn.setOnClickListener(v -> employeeLogin());
        rootLayout.addView(employeeBtn);
    }

    // ---------------- ADMIN ----------------
    void adminLogin() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Admin Login");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        // If company not created, create with USD as default currency
        if(company == null){
            company = new Company("DemoCompany", "USD");
            Toast.makeText(this, "Company created with currency USD", Toast.LENGTH_SHORT).show();
        }

        loggedInUser = new User("admin", Role.ADMIN, null);
        users.put("admin", loggedInUser);

        Button createUserBtn = new Button(this);
        createUserBtn.setText("Create Employee / Manager");
        createUserBtn.setOnClickListener(v -> showCreateUser());
        rootLayout.addView(createUserBtn);

        Button viewUsersBtn = new Button(this);
        viewUsersBtn.setText("View All Users");
        viewUsersBtn.setOnClickListener(v -> showUserList());
        rootLayout.addView(viewUsersBtn);

        Button viewExpensesBtn = new Button(this);
        viewExpensesBtn.setText("View All Expenses");
        viewExpensesBtn.setOnClickListener(v -> showAllExpenses());
        rootLayout.addView(viewExpensesBtn);

        Button logoutBtn = new Button(this);
        logoutBtn.setText("Logout");
        logoutBtn.setOnClickListener(v -> { loggedInUser = null; showRoleSelection(); });
        rootLayout.addView(logoutBtn);
    }

    void showCreateUser(){
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Create User");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        rootLayout.addView(usernameInput);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Employee", "Manager"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        rootLayout.addView(roleSpinner);

        Spinner managerSpinner = new Spinner(this);
        rootLayout.addView(managerSpinner);
        updateManagerSpinner(managerSpinner);

        Button createBtn = new Button(this);
        createBtn.setText("Create");
        createBtn.setOnClickListener(v -> {
            String uname = usernameInput.getText().toString().trim();
            if(uname.isEmpty()){
                Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show();
                return;
            }
            if(users.containsKey(uname)){
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            Role role = roleSpinner.getSelectedItemPosition() == 0 ? Role.EMPLOYEE : Role.MANAGER;
            User manager = null;
            if(role == Role.EMPLOYEE && managerSpinner.getSelectedItemPosition() > 0){
                String mgrName = (String) managerSpinner.getSelectedItem();
                manager = users.get(mgrName);
            }
            User newUser = new User(uname, role, manager);
            users.put(uname, newUser);
            Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show();
            showCreateUser();
        });
        rootLayout.addView(createBtn);

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> adminLogin());
        rootLayout.addView(backBtn);
    }

    void updateManagerSpinner(Spinner spinner){
        List<String> mgrNames = new ArrayList<>();
        mgrNames.add("No Manager");
        for(User u : users.values()){
            if(u.role == Role.MANAGER){
                mgrNames.add(u.username);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mgrNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    void showUserList(){
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("All Users");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        for(User u : users.values()){
            TextView userTv = new TextView(this);
            String mgrName = u.manager == null ? "None" : u.manager.username;
            userTv.setText(u.username + " - " + u.role + " - Manager: " + mgrName);
            rootLayout.addView(userTv);
        }

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> adminLogin());
        rootLayout.addView(backBtn);
    }

    void showAllExpenses(){
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("All Expenses");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        if(company.expenses.isEmpty()){
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expenses submitted.");
            rootLayout.addView(emptyTv);
        } else {
            for(Expense e : company.expenses){
                TextView expTv = new TextView(this);
                expTv.setText(e.toString());
                rootLayout.addView(expTv);
                rootLayout.addView(new View(this){{
                    setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                    setBackgroundColor(0xFFAAAAAA);
                }});
            }
        }

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> adminLogin());
        rootLayout.addView(backBtn);
    }

    // -------------- MANAGER -------------------
    void managerLogin() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Enter your Manager Username:");
        tv.setTextSize(18);
        rootLayout.addView(tv);

        EditText usernameInput = new EditText(this);
        rootLayout.addView(usernameInput);

        Button loginBtn = new Button(this);
        loginBtn.setText("Login");
        loginBtn.setOnClickListener(v -> {
            String uname = usernameInput.getText().toString().trim();
            if(!users.containsKey(uname) || users.get(uname).role != Role.MANAGER){
                Toast.makeText(this, "Manager not found", Toast.LENGTH_SHORT).show();
                return;
            }
            loggedInUser = users.get(uname);
            showManagerDashboard();
        });
        rootLayout.addView(loginBtn);

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showRoleSelection());
        rootLayout.addView(backBtn);
    }

    void showManagerDashboard() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Manager: " + loggedInUser.username);
        tv.setTextSize(20);
        rootLayout.addView(tv);

        Button viewPendingBtn = new Button(this);
        viewPendingBtn.setText("View Expenses to Approve");
        viewPendingBtn.setOnClickListener(v -> showManagerApprovals());
        rootLayout.addView(viewPendingBtn);

        Button viewTeamExpensesBtn = new Button(this);
        viewTeamExpensesBtn.setText("View Team Expenses");
        viewTeamExpensesBtn.setOnClickListener(v -> showTeamExpenses());
        rootLayout.addView(viewTeamExpensesBtn);

        Button logoutBtn = new Button(this);
        logoutBtn.setText("Logout");
        logoutBtn.setOnClickListener(v -> { loggedInUser=null; showRoleSelection(); });
        rootLayout.addView(logoutBtn);
    }

    void showManagerApprovals() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Expenses Waiting For Your Approval");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        boolean found = false;
        for(Expense e : company.expenses){
            if(e.needsApprovalBy(loggedInUser)){
                found = true;
                showExpenseApproval(e);
            }
        }
        if(!found){
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expenses to approve.");
            rootLayout.addView(emptyTv);
        }

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showManagerDashboard());
        rootLayout.addView(backBtn);
    }

    void showExpenseApproval(Expense e) {
        TextView expTv = new TextView(this);
        expTv.setText(e.toString());
        rootLayout.addView(expTv);

        EditText commentInput = new EditText(this);
        commentInput.setHint("Add comment");
        rootLayout.addView(commentInput);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button approveBtn = new Button(this);
        approveBtn.setText("Approve");
        approveBtn.setOnClickListener(v -> {
            e.approve(loggedInUser, commentInput.getText().toString());
            Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show();
            showManagerApprovals();
        });
        btnLayout.addView(approveBtn);

        Button rejectBtn = new Button(this);
        rejectBtn.setText("Reject");
        rejectBtn.setOnClickListener(v -> {
            e.reject(loggedInUser, commentInput.getText().toString());
            Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show();
            showManagerApprovals();
        });
        btnLayout.addView(rejectBtn);

        rootLayout.addView(btnLayout);
    }

    void showTeamExpenses() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Team Expenses");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        boolean found = false;
        for(Expense e : company.expenses){
            if(e.submitter.manager != null && e.submitter.manager.username.equals(loggedInUser.username)){
                found = true;
                TextView expTv = new TextView(this);
                expTv.setText(e.toString());
                rootLayout.addView(expTv);
            }
        }

        if(!found){
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No team expenses.");
            rootLayout.addView(emptyTv);
        }

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showManagerDashboard());
        rootLayout.addView(backBtn);
    }

    // ---------------- EMPLOYEE -----------------
    void employeeLogin() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Enter your Employee Username:");
        tv.setTextSize(18);
        rootLayout.addView(tv);

        EditText usernameInput = new EditText(this);
        rootLayout.addView(usernameInput);

        Button loginBtn = new Button(this);
        loginBtn.setText("Login");
        loginBtn.setOnClickListener(v -> {
            String uname = usernameInput.getText().toString().trim();
            if(!users.containsKey(uname) || users.get(uname).role != Role.EMPLOYEE){
                Toast.makeText(this, "Employee not found", Toast.LENGTH_SHORT).show();
                return;
            }
            loggedInUser = users.get(uname);
            showEmployeeDashboard();
        });
        rootLayout.addView(loginBtn);

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showRoleSelection());
        rootLayout.addView(backBtn);
    }

    void showEmployeeDashboard() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Employee: " + loggedInUser.username);
        tv.setTextSize(20);
        rootLayout.addView(tv);

        Button submitExpenseBtn = new Button(this);
        submitExpenseBtn.setText("Submit Expense");
        submitExpenseBtn.setOnClickListener(v -> showExpenseSubmission());
        rootLayout.addView(submitExpenseBtn);

        Button viewHistoryBtn = new Button(this);
        viewHistoryBtn.setText("View Expense History");
        viewHistoryBtn.setOnClickListener(v -> showExpenseHistory());
        rootLayout.addView(viewHistoryBtn);

        Button logoutBtn = new Button(this);
        logoutBtn.setText("Logout");
        logoutBtn.setOnClickListener(v -> { loggedInUser=null; showRoleSelection(); });
        rootLayout.addView(logoutBtn);
    }

    void showExpenseSubmission() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Submit Expense");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rootLayout.addView(amountInput);

        EditText currencyInput = new EditText(this);
        currencyInput.setHint("Currency (e.g., USD, EUR)");
        rootLayout.addView(currencyInput);

        Spinner categorySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Travel", "Food", "Supplies", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        rootLayout.addView(categorySpinner);

        EditText descInput = new EditText(this);
        descInput.setHint("Description");
        rootLayout.addView(descInput);

        EditText dateInput = new EditText(this);
        dateInput.setHint("Date (YYYY-MM-DD)");
        rootLayout.addView(dateInput);

        Button submitBtn = new Button(this);
        submitBtn.setText("Submit");
        submitBtn.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String currency = currencyInput.getText().toString().trim().toUpperCase(Locale.ROOT);
            String category = (String) categorySpinner.getSelectedItem();
            String desc = descInput.getText().toString().trim();
            String date = dateInput.getText().toString().trim();

            if(amountStr.isEmpty() || currency.isEmpty() || desc.isEmpty() || date.isEmpty()){
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (Exception e){
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert to company currency
            new CurrencyConverterTask(amount, currency, company.currency, convertedAmount -> {
                Expense exp = new Expense(loggedInUser, amount, currency, convertedAmount, category, desc, date);
                company.expenses.add(exp);
                Toast.makeText(this, "Expense submitted", Toast.LENGTH_SHORT).show();
                showEmployeeDashboard();
            }).execute();
        });
        rootLayout.addView(submitBtn);

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showEmployeeDashboard());
        rootLayout.addView(backBtn);
    }

    void showExpenseHistory() {
        rootLayout.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText("Your Expense History");
        tv.setTextSize(20);
        rootLayout.addView(tv);

        boolean found = false;
        for(Expense e : company.expenses){
            if(e.submitter.username.equals(loggedInUser.username)){
                found = true;
                TextView expTv = new TextView(this);
                expTv.setText(e.toString());
                rootLayout.addView(expTv);
            }
        }
        if(!found){
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expenses submitted.");
            rootLayout.addView(emptyTv);
        }

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setOnClickListener(v -> showEmployeeDashboard());
        rootLayout.addView(backBtn);
    }

    // --------- DATA MODELS -----------

    enum Role {ADMIN, MANAGER, EMPLOYEE}

    class User {
        String username;
        Role role;
        User manager;  // Null if no manager

        User(String username, Role role, User manager) {
            this.username = username;
            this.role = role;
            this.manager = manager;
        }
    }

    class Company {
        String name;
        String currency;
        List<Expense> expenses = new ArrayList<>();

        Company(String name, String currency){
            this.name = name;
            this.currency = currency;
        }
    }

    class Expense {
        User submitter;
        double amountOriginal;
        String currencyOriginal;
        double amountCompanyCurrency;
        String category;
        String description;
        String date;
        Map<User, Approval> approvals = new LinkedHashMap<>();
        boolean isRejected = false;

        Expense(User submitter, double amountOriginal, String currencyOriginal, double amountCompanyCurrency,
                String category, String description, String date) {
            this.submitter = submitter;
            this.amountOriginal = amountOriginal;
            this.currencyOriginal = currencyOriginal;
            this.amountCompanyCurrency = amountCompanyCurrency;
            this.category = category;
            this.description = description;
            this.date = date;

            // Setup approval flow
            setupApprovalFlow();
        }

        private void setupApprovalFlow(){
            // If employee has a manager and manager is approver, add
            if(submitter.manager != null){
                approvals.put(submitter.manager, new Approval());
            }
            // Admin is final approver
            User adminUser = users.get("admin");
            if(adminUser != null && !approvals.containsKey(adminUser)) {
                approvals.put(adminUser, new Approval());
            }
        }

        boolean needsApprovalBy(User u){
            if(isRejected) return false;
            // Approvals are sequential
            for(Map.Entry<User, Approval> entry : approvals.entrySet()){
                User approver = entry.getKey();
                Approval approval = entry.getValue();
                if(!approval.isDecided()){
                    return approver.username.equals(u.username);
                }
                if(approval.isRejected()){
                    return false;
                }
            }
            return false;
        }

        void approve(User u, String comment){
            Approval app = approvals.get(u);
            if(app != null && !app.isDecided()){
                app.approved = true;
                app.comment = comment;
            }
        }

        void reject(User u, String comment){
            Approval app = approvals.get(u);
            if(app != null && !app.isDecided()){
                app.approved = false;
                app.comment = comment;
                isRejected = true;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Submitter: ").append(submitter.username).append("\n");
            sb.append("Amount: ").append(String.format(Locale.US, "%.2f", amountOriginal)).append(" ").append(currencyOriginal)
                    .append(" (").append(String.format(Locale.US, "%.2f", amountCompanyCurrency)).append(" ").append(company.currency).append(")\n");
            sb.append("Category: ").append(category).append("\n");
            sb.append("Description: ").append(description).append("\n");
            sb.append("Date: ").append(date).append("\n");
            sb.append("Approvals:\n");
            for(Map.Entry<User, Approval> entry : approvals.entrySet()){
                User approver = entry.getKey();
                Approval approval = entry.getValue();
                sb.append(" - ").append(approver.username).append(": ");
                if(!approval.isDecided()){
                    sb.append("Pending");
                } else if(approval.isRejected()){
                    sb.append("Rejected (").append(approval.comment).append(")");
                } else {
                    sb.append("Approved (").append(approval.comment).append(")");
                }
                sb.append("\n");
            }
            if(isRejected) sb.append("Status: Rejected\n");
            else if(approvals.values().stream().allMatch(Approval::isApproved)) sb.append("Status: Approved\n");
            else sb.append("Status: Pending\n");

            return sb.toString();
        }
    }

    class Approval {
        Boolean approved = null;
        String comment = "";

        boolean isDecided() { return approved != null; }
        boolean isApproved() { return approved != null && approved; }
        boolean isRejected() { return approved != null && !approved; }
    }

    // ----------- Async Task for currency conversion ----------------

    interface ConversionCallback {
        void onResult(double convertedAmount);
    }

    class CurrencyConverterTask extends AsyncTask<Void, Void, Double> {

        double amount;
        String fromCurrency, toCurrency;
        ConversionCallback callback;

        CurrencyConverterTask(double amount, String fromCurrency, String toCurrency, ConversionCallback callback){
            this.amount = amount;
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
            this.callback = callback;
        }

        @Override
        protected Double doInBackground(Void... voids) {
            try {
                String urlStr = "https://api.exchangerate-api.com/v4/latest/" + fromCurrency;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);

                int code = conn.getResponseCode();
                if(code != 200) return null;

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }
                reader.close();

                JSONObject obj = new JSONObject(sb.toString());
                JSONObject rates = obj.getJSONObject("rates");
                if(!rates.has(toCurrency)) return null;
                double rate = rates.getDouble(toCurrency);

                return amount * rate;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Double converted) {
            if(converted == null) {
                Toast.makeText(MainActivity.this, "Currency conversion failed, using original amount", Toast.LENGTH_SHORT).show();
                callback.onResult(amount);
            } else {
                callback.onResult(converted);
            }
        }
    }
}
