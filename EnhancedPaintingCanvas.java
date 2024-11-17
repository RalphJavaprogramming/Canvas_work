package com.example.canvas_with_sql;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;

public class EnhancedPaintingCanvas extends JFrame {
    private BufferedImage canvasImage;
    private Graphics2D g2d;
    private int prevX, prevY;
    private Color currentColor = Color.BLACK;
    private Color fillColor = Color.WHITE;
    private int brushSize = 2;
    private String currentTool = "Pencil";
    private boolean fill = false;
    private int eraserSize = 10;

    public EnhancedPaintingCanvas() {
        setTitle("Enhanced Painting Canvas with Tools");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton colorButton = new JButton("Stroke Color");
        colorButton.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(null, "Choose a Stroke Color", currentColor);
            if (chosenColor != null) {
                currentColor = chosenColor;
                if (g2d != null) {
                    g2d.setColor(currentColor);
                }
            }
        });
        controlPanel.add(colorButton);

        JButton fillColorButton = new JButton("Fill Color");
        fillColorButton.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(null, "Choose a Fill Color", fillColor);
            if (chosenColor != null) {
                fillColor = chosenColor;
            }
        });
        controlPanel.add(fillColorButton);

        JSlider brushSlider = new JSlider(1, 20, brushSize);
        brushSlider.setMajorTickSpacing(5);
        brushSlider.setPaintTicks(true);
        brushSlider.setPaintLabels(true);
        brushSlider.addChangeListener(e -> {
            brushSize = brushSlider.getValue();
            if (g2d != null) {
                g2d.setStroke(new BasicStroke(brushSize));
            }
        });
        controlPanel.add(new JLabel("Brush Size:"));
        controlPanel.add(brushSlider);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearCanvas());
        controlPanel.add(clearButton);

        JButton pencilButton = new JButton("Pencil");
        JButton rectButton = new JButton("Rectangle");
        JButton circleButton = new JButton("Circle");
        JButton triangleButton = new JButton("Triangle");
        JButton fillButton = new JButton("Fill");

        pencilButton.addActionListener(e -> {
            currentTool = "Pencil";
            fill = false;
        });
        rectButton.addActionListener(e -> {
            currentTool = "Rectangle";
            fill = false;
        });
        circleButton.addActionListener(e -> {
            currentTool = "Circle";
            fill = false;
        });
        triangleButton.addActionListener(e -> {
            currentTool = "Triangle";
            fill = false;
        });
        fillButton.addActionListener(e -> {
            fill = !fill;
            fillButton.setText(fill ? "Fill: ON" : "Fill: OFF");
        });

        controlPanel.add(pencilButton);
        controlPanel.add(rectButton);
        controlPanel.add(circleButton);
        controlPanel.add(triangleButton);
        controlPanel.add(fillButton);

        JButton eraserButton = new JButton("Eraser");
        eraserButton.addActionListener(e -> {
            currentTool = "Eraser";
            fill = false;
        });
        controlPanel.add(eraserButton);

        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e -> {
            currentTool = "Line";
            fill = false;
        });
        controlPanel.add(lineButton);

        JSlider eraserSlider = new JSlider(1, 50, eraserSize);
        eraserSlider.setMajorTickSpacing(10);
        eraserSlider.setPaintTicks(true);
        eraserSlider.setPaintLabels(true);
        eraserSlider.addChangeListener(e -> {
            eraserSize = eraserSlider.getValue();
        });
        controlPanel.add(new JLabel("Eraser Size:"));
        controlPanel.add(eraserSlider);

        JButton saveButton = new JButton("Save to DB");
        saveButton.addActionListener(e -> saveImageToDatabase());
        controlPanel.add(saveButton);

        JButton loadButton = new JButton("Load from DB");
        loadButton.addActionListener(e -> loadImagesFromDatabase());
        controlPanel.add(loadButton);

        JButton importButton = new JButton("Import Image");
        importButton.addActionListener(e -> importImage());
        controlPanel.add(importButton);

        DrawingPanel drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private void clearCanvas() {
        if (canvasImage != null) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(currentColor);
            repaint();
        }
    }

    private void importImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".png")
                        || f.getName().toLowerCase().endsWith(".jpg")
                        || f.getName().toLowerCase().endsWith(".jpeg")
                        || f.isDirectory();
            }

            public String getDescription() {
                return "Image files (*.png, *.jpg, *.jpeg)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage importedImage = ImageIO.read(selectedFile);
                canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2d = (Graphics2D) canvasImage.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(importedImage, 0, 0, getWidth(), getHeight(), null);
                repaint();
                saveImageToDatabase();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error importing image: " + e.getMessage());
            }
        }
    }

    private void saveImageToDatabase() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvasImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/painting_app", "root", "your_root_password");
            String sql = "INSERT INTO images (image_data) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setBytes(1, imageBytes);
            pstmt.executeUpdate();
            conn.close();

            JOptionPane.showMessageDialog(this, "Image saved to database successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving image to database: " + e.getMessage());
        }
    }

    private void loadImagesFromDatabase() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/painting_app", "root", "your_root_password");
            String sql = "SELECT id, image_data FROM images";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            JPanel imagePanel = new JPanel(new GridLayout(0, 3, 10, 10));
            while (rs.next()) {
                int id = rs.getInt("id");
                byte[] imageBytes = rs.getBytes("image_data");
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

                JButton imgButton = new JButton(new ImageIcon(img.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                imgButton.addActionListener(e -> loadImageToCanvas(img));
                imagePanel.add(imgButton);
            }
            conn.close();

            JScrollPane scrollPane = new JScrollPane(imagePanel);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            JOptionPane.showMessageDialog(this, scrollPane, "Select an Image", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading images from database: " + e.getMessage());
        }
    }

    private void loadImageToCanvas(BufferedImage img) {
        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        g2d = (Graphics2D) canvasImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        repaint();
    }

    private class DrawingPanel extends JPanel {
        public DrawingPanel() {
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    prevX = e.getX();
                    prevY = e.getY();

                    if (canvasImage == null) {
                        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                        g2d = (Graphics2D) canvasImage.getGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setStroke(new BasicStroke(brushSize));
                        g2d.setColor(currentColor);
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();

                    switch (currentTool) {
                        case "Rectangle" -> {
                            if (fill) {
                                g2d.setColor(fillColor);
                                g2d.fillRect(Math.min(prevX, x), Math.min(prevY, y), Math.abs(x - prevX), Math.abs(y - prevY));
                                g2d.setColor(currentColor);
                            }
                            g2d.drawRect(Math.min(prevX, x), Math.min(prevY, y), Math.abs(x - prevX), Math.abs(y - prevY));
                        }
                        case "Circle" -> {
                            int diameter = Math.max(Math.abs(x - prevX), Math.abs(y - prevY));
                            if (fill) {
                                g2d.setColor(fillColor);
                                g2d.fillOval(Math.min(prevX, x), Math.min(prevY, y), diameter, diameter);
                                g2d.setColor(currentColor);
                            }
                            g2d.drawOval(Math.min(prevX, x), Math.min(prevY, y), diameter, diameter);
                        }
                        case "Triangle" -> {
                            int[] xPoints = {prevX, x, (prevX + x) / 2};
                            int[] yPoints = {prevY, prevY, y};
                            if (fill) {
                                g2d.setColor(fillColor);
                                g2d.fillPolygon(xPoints, yPoints, 3);
                                g2d.setColor(currentColor);
                            }
                            g2d.drawPolygon(xPoints, yPoints, 3);
                        }
                        case "Line" -> {
                            g2d.drawLine(prevX, prevY, x, y);
                        }
                    }
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    int currX = e.getX();
                    int currY = e.getY();

                    if (currentTool.equals("Pencil")) {
                        g2d.drawLine(prevX, prevY, currX, currY);
                    } else if (currentTool.equals("Eraser")) {
                        g2d.setColor(Color.WHITE);
                        g2d.fillOval(currX - eraserSize/2, currY - eraserSize/2, eraserSize, eraserSize);
                        g2d.setColor(currentColor);
                    }

                    prevX = currX;
                    prevY = currY;

                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvasImage != null) {
                g.drawImage(canvasImage, 0, 0, null);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnhancedPaintingCanvas::new);
    }
}