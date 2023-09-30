import java.sql.*;
import java.util.Scanner;

public class LibraryManagementSystem {

    static Connection connection;

    public static void main(String[] args) {
        try {
            // Establish connection to the database
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/library", "username", "password");

            // Create tables if they don't exist
            createTables();

            // Show menu
            showMenu();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void createTables() throws SQLException {
        Statement statement = connection.createStatement();

        // Create tables for books and patrons if they don't exist
        String createBooksTable = "CREATE TABLE IF NOT EXISTS books (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), author VARCHAR(255), genre VARCHAR(255), is_available BOOLEAN)";
        statement.executeUpdate(createBooksTable);

        String createPatronsTable = "CREATE TABLE IF NOT EXISTS patrons (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), contact VARCHAR(255))";
        statement.executeUpdate(createPatronsTable);

        // Create table for borrowing records if it doesn't exist
        String createBorrowingTable = "CREATE TABLE IF NOT EXISTS borrowing (id INT AUTO_INCREMENT PRIMARY KEY, book_id INT, patron_id INT, borrow_date DATE, return_date DATE, FOREIGN KEY (book_id) REFERENCES books(id), FOREIGN KEY (patron_id) REFERENCES patrons(id))";
        statement.executeUpdate(createBorrowingTable);

        statement.close();
    }

    static void showMenu() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("Library Management System");
            System.out.println("1. Add Book");
            System.out.println("2. Add Patron");
            System.out.println("3. Borrow Book");
            System.out.println("4. Return Book");
            System.out.println("5. Search Books");
            System.out.println("6. Search Patrons");
            System.out.println("7. Generate Reports");
            System.out.println("8. Quit");
            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    addBook();
                    break;
                case 2:
                    addPatron();
                    break;
                case 3:
                    borrowBook();
                    break;
                case 4:
                    returnBook();
                    break;
                case 5:
                    searchBooks();
                    break;
                case 6:
                    searchPatrons();
                    break;
                case 7:
                    generateReports();
                    break;
                case 8:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

        } while (choice != 8);

        scanner.close();
        connection.close();
    }

    static void addBook() throws SQLException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter title: ");
        String title = scanner.nextLine();
        System.out.print("Enter author: ");
        String author = scanner.nextLine();
        System.out.print("Enter genre: ");
        String genre = scanner.nextLine();

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO books (title, author, genre, is_available) VALUES (?, ?, ?, ?)");
        preparedStatement.setString(1, title);
        preparedStatement.setString(2, author);
        preparedStatement.setString(3, genre);
        preparedStatement.setBoolean(4, true);
        preparedStatement.executeUpdate();

        System.out.println("Book added successfully.");
    }

    static void addPatron() throws SQLException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter name: ");
        String name = scanner.nextLine();
        System.out.print("Enter contact information: ");
        String contact = scanner.nextLine();

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO patrons (name, contact) VALUES (?, ?)");
        preparedStatement.setString(1, name);
        preparedStatement.setString(2, contact);
        preparedStatement.executeUpdate();

        System.out.println("Patron added successfully.");
    }

    static void borrowBook() throws SQLException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter patron ID: ");
        int patronId = scanner.nextInt();
        System.out.print("Enter book ID: ");
        int bookId = scanner.nextInt();

        // Check if book is available
        PreparedStatement checkAvailability = connection.prepareStatement("SELECT is_available FROM books WHERE id = ?");
        checkAvailability.setInt(1, bookId);
        ResultSet resultSet = checkAvailability.executeQuery();
        resultSet.next();
        boolean isAvailable = resultSet.getBoolean("is_available");

        if (!isAvailable) {
            System.out.println("Book is not available for borrowing.");
            return;
        }

        // Update book availability
        PreparedStatement updateAvailability = connection.prepareStatement("UPDATE books SET is_available = ? WHERE id = ?");
        updateAvailability.setBoolean(1, false);
        updateAvailability.setInt(2, bookId);
        updateAvailability.executeUpdate();

        // Get current date
        Date borrowDate = new Date(System.currentTimeMillis());

        // Insert borrowing record
        PreparedStatement insertRecord = connection.prepareStatement("INSERT INTO borrowing (book_id, patron_id, borrow_date) VALUES (?, ?, ?)");
        insertRecord.setInt(1, bookId);
        insertRecord.setInt(2, patronId);
        insertRecord.setDate(3, borrowDate);
        insertRecord.executeUpdate();

        System.out.println("Book borrowed successfully.");
    }

    static void returnBook() throws SQLException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter book ID: ");
        int bookId = scanner.nextInt();

        // Check if book is borrowed
        PreparedStatement checkBorrowed = connection.prepareStatement("SELECT * FROM borrowing WHERE book_id = ?");
        checkBorrowed.setInt(1, bookId);
        ResultSet resultSet = checkBorrowed.executeQuery();

        if (!resultSet.next()) {
            System.out.println("Book is not currently borrowed.");
            return;
        }

        // Get current date
        Date returnDate = new Date(System.currentTimeMillis());

        // Update borrowing record
        PreparedStatement updateRecord = connection.prepareStatement("UPDATE borrowing SET return_date = ? WHERE book_id = ? AND return_date IS NULL");
        updateRecord.setDate(1, returnDate);
        updateRecord.setInt(2, bookId);
        updateRecord.executeUpdate();

        // Update book availability
        PreparedStatement updateAvailability = connection.prepareStatement("UPDATE books SET is_available = ? WHERE id = ?");
        updateAvailability.setBoolean(1, true);
        updateAvailability.setInt(2, bookId);
        updateAvailability.executeUpdate();

        System.out.println("Book returned successfully.");
    }

    static void searchBooks() throws SQLException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter search query (title/author/genre): ");
        String query = scanner.nextLine();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR genre LIKE ?");
        preparedStatement.setString(1, "%" + query + "%");
        preparedStatement.setString(2, "%" + query + "%");
        preparedStatement.setString(3, "%" + query + "%");

        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            System.out.println("ID: " + resultSet.getInt("id") +
                    ", Title: " + resultSet.getString("title") +
                    ", Author: " + resultSet.getString("author") +
                    ", Genre: " + resultSet.getString("genre") +
                    ", Availability
