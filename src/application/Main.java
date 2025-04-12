package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class Main extends Application {
    private PrintWriter out;
    private final boolean[] localChange = {false};
   
    @Override
    public void start(Stage primaryStage) {
    	TextInputDialog dialog = new TextInputDialog("User");
    	dialog.setTitle("Enter Username");
    	dialog.setHeaderText("Welcome to the Collab Editor");
    	dialog.setContentText("Please enter your name:");

    	Optional<String> result = dialog.showAndWait();
    	String username = result.orElse("User");

        try {
            // UI Setup
            TextArea textEditor = new TextArea();
            textEditor.setPrefHeight(300);

            TextArea chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setWrapText(true);
            chatArea.setPrefHeight(150);

            TextField chatInput = new TextField();
            chatInput.setPromptText("Type a message...");
         // Create the buttons for Send, Clear, and Save
            Button sendBtn = new Button("Send");
            Button clearChatBtn = new Button("Clear");
            Button saveBtn = new Button("Save File");
           


            // Add buttons to a horizontal layout (HBox)
            HBox chatControls = new HBox(10, sendBtn, clearChatBtn, saveBtn);
            chatControls.setPadding(new Insets(5, 0, 0, 0));  // Add space around the buttons


           
            VBox chatBox = new VBox(5, new Label("Chat"), chatArea, chatInput, chatControls);

            chatBox.setPadding(new Insets(10));

            VBox editorBox = new VBox(5, new Label("Collaborative Editor"), textEditor); 
            editorBox.setPadding(new Insets(10));
            Label title = new Label("📄 CollabText - Real-Time Editor");
            title.setStyle("-fx-font-size: 24px; -fx-text-fill: #00ffe4; -fx-font-weight: bold;");
            title.setPadding(new Insets(10));


            VBox root = new VBox(10, title, editorBox, chatBox);

            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Real-Time Collab Editor with Chat");
            primaryStage.show();
            
            
            

            
            sendBtn.setOnAction(e -> {
                String msg = chatInput.getText();
                if (!msg.isEmpty()) {
                    out.println("CHAT:[" + username + "]: " + msg);  // Send message to server
                    chatInput.clear();  // Clear the input field after sending
                }
            });
            

            

            clearChatBtn.setOnAction(e -> chatArea.clear());  // Clears the chat area
            saveBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Collaborative Text");
                fileChooser.setInitialFileName("document.txt");
                File file = fileChooser.showSaveDialog(primaryStage);  // Open save dialog
                if (file != null) {
                    try (PrintWriter writer = new PrintWriter(file)) {
                        writer.print(textEditor.getText());  // Save the content of the text editor to the file
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            



            // Connect to server
            Socket socket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Handle text updates
            textEditor.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!localChange[0]) {
                    out.println("TEXT:" + newVal);
                }
                localChange[0] = false;
            });

            // Handle chat input
            chatInput.setOnAction(e -> {
                String msg = chatInput.getText();
                if (!msg.isEmpty()) {
                    out.println("CHAT:[" + username + "]: " + msg);
                    chatInput.clear();
                }
            });

            // Incoming messages
            Thread reader = new Thread(() -> {
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("TEXT:")) {
                            String content = line.substring(5);
                            Platform.runLater(() -> {
                                if (!textEditor.getText().equals(content)) {
                                    int caretPos = textEditor.getCaretPosition();
                                    localChange[0] = true;
                                    textEditor.setText(content);
                                    textEditor.positionCaret(Math.min(caretPos, content.length()));
                                }
                            });

                        } else if (line.startsWith("CHAT:")) {
                            String message = line.substring(5);
                            Platform.runLater(() -> chatArea.appendText(message + "\n"));
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected.");
                }
            });
            reader.setDaemon(true);
            reader.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
