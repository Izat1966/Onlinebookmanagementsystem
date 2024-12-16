package org.example;

import java.sql.*;
import java.util.Scanner;

import static org.example.BookStoreManagement.*;

public class Main {
    public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== Online Bookstore Management System ===");
                System.out.println("1. Add Book");
                System.out.println("2. View All Books");
                System.out.println("3. Add Customer");
                System.out.println("4. Place an Order");
                System.out.println("5. View Orders");
                System.out.println("6. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> viewBooks();
                    case 3 -> addCustomer();
                    case 4 -> placeOrder();
                    case 5 -> viewOrders();
                    case 6 -> {
                        System.out.println("Exiting... Goodbye!");
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            }
        }

        private static void addBook() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Scanner scanner = new Scanner(System.in)) {

                System.out.print("Enter ISBN: ");
                String isbn = scanner.nextLine();
                System.out.print("Enter Title: ");
                String title = scanner.nextLine();
                System.out.print("Enter Genre: ");
                String genre = scanner.nextLine();
                System.out.print("Enter Price: ");
                double price = scanner.nextDouble();
                System.out.print("Enter Stock: ");
                int stock = scanner.nextInt();

                String sql = "INSERT INTO Book (ISBN, Title, Genre, Price, Stock) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, isbn);
                    stmt.setString(2, title);
                    stmt.setString(3, genre);
                    stmt.setDouble(4, price);
                    stmt.setInt(5, stock);
                    stmt.executeUpdate();
                    System.out.println("Book added successfully!");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static void viewBooks() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {

                String sql = "SELECT * FROM Book";
                ResultSet rs = stmt.executeQuery(sql);

                System.out.println("\n=== Book List ===");
                while (rs.next()) {
                    System.out.printf("ISBN: %s, Title: %s, Genre: %s, Price: %.2f, Stock: %d%n",
                            rs.getString("ISBN"), rs.getString("Title"),
                            rs.getString("Genre"), rs.getDouble("Price"),
                            rs.getInt("Stock"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static void addCustomer() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Scanner scanner = new Scanner(System.in)) {

                System.out.print("Enter Customer Name: ");
                String name = scanner.nextLine();
                System.out.print("Enter Customer Email: ");
                String email = scanner.nextLine();

                String sql = "INSERT INTO Customer (Name, Email) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    stmt.executeUpdate();
                    System.out.println("Customer added successfully!");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static void placeOrder() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Scanner scanner = new Scanner(System.in)) {

                System.out.print("Enter Customer ID: ");
                int customerId = scanner.nextInt();

                System.out.print("Enter ISBN of the Book: ");
                String isbn = scanner.next();
                System.out.print("Enter Quantity: ");
                int quantity = scanner.nextInt();

                conn.setAutoCommit(false); // Start transaction

                String checkStockSql = "SELECT Stock, Price FROM Book WHERE ISBN = ?";
                try (PreparedStatement checkStockStmt = conn.prepareStatement(checkStockSql)) {
                    checkStockStmt.setString(1, isbn);
                    ResultSet rs = checkStockStmt.executeQuery();

                    if (rs.next()) {
                        int stock = rs.getInt("Stock");
                        double price = rs.getDouble("Price");

                        if (stock >= quantity) {
                            double totalAmount = quantity * price;

                            String orderSql = "INSERT INTO Orders (Customer_ID, Total_Amount) VALUES (?, ?)";
                            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                                orderStmt.setInt(1, customerId);
                                orderStmt.setDouble(2, totalAmount);
                                orderStmt.executeUpdate();

                                ResultSet generatedKeys = orderStmt.getGeneratedKeys();
                                if (generatedKeys.next()) {
                                    int orderId = generatedKeys.getInt(1);

                                    String bookOrderSql = "INSERT INTO Book_Order (Order_ID, ISBN, Quantity) VALUES (?, ?, ?)";
                                    try (PreparedStatement bookOrderStmt = conn.prepareStatement(bookOrderSql)) {
                                        bookOrderStmt.setInt(1, orderId);
                                        bookOrderStmt.setString(2, isbn);
                                        bookOrderStmt.setInt(3, quantity);
                                        bookOrderStmt.executeUpdate();
                                    }

                                    String updateStockSql = "UPDATE Book SET Stock = Stock - ? WHERE ISBN = ?";
                                    try (PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql)) {
                                        updateStockStmt.setInt(1, quantity);
                                        updateStockStmt.setString(2, isbn);
                                        updateStockStmt.executeUpdate();
                                    }

                                    conn.commit();
                                    System.out.println("Order placed successfully!");
                                }
                            }
                        } else {
                            System.out.println("Not enough stock available.");
                        }
                    } else {
                        System.out.println("Book not found.");
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    e.printStackTrace();
                } finally {
                    conn.setAutoCommit(true); // End transaction
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static void viewOrders() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {

                String sql = "SELECT * FROM Orders";
                ResultSet rs = stmt.executeQuery(sql);

                System.out.println("\n=== Order List ===");
                while (rs.next()) {
                    System.out.printf("Order ID: %d, Customer ID: %d, Order Date: %s, Total Amount: %.2f%n",
                            rs.getInt("Order_ID"), rs.getInt("Customer_ID"),
                            rs.getTimestamp("Order_Date"), rs.getDouble("Total_Amount"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
