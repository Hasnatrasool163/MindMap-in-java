
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.*;

class MindMapNode {
    String text;
    int x, y;
    int width, height;
    Color color;
    boolean isSelected=false;
    List<MindMapNode> children = new ArrayList<>();

    MindMapNode(String text, int x, int y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        adjustSize();
    }

    void adjustSize() {
        int baseWidth = 100;
        int baseHeight = 50;
        width = Math.max(baseWidth, text.length() * 7);
        height = baseHeight;
    }

    void setText(String text) {
        this.text = text;
        adjustSize();
    }

    boolean containsPoint(int x, int y) {
        return x > this.x && x < this.x + width && y > this.y && y < this.y + height;
    }

    void addChild(MindMapNode child) {
        children.add(child);
    }

    void toggleSelection() {
        isSelected = !isSelected;
    }

    void setColor(Color color) {
        this.color = color;
    }
}

class MindMapSurface extends JPanel {
    private List<MindMapNode> nodes = new ArrayList<>();
    private MindMapNode selectedNode = null;

    MindMapSurface() {
        setComponentPopupMenu(createPopupMenu());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    for (MindMapNode node : nodes) {
                        if (node.containsPoint(e.getX(), e.getY())) {
                            String newText = JOptionPane.showInputDialog(null, "Edit Node Text:", node.text);
                            if (newText != null && !newText.trim().isEmpty()) {
                                node.setText(newText);
                                repaint();
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    for (MindMapNode node : nodes) {
                        if (node.containsPoint(e.getX(), e.getY())) {
                            selectedNode = node;
                            return;
                        }
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    for (MindMapNode node : nodes) {
                        if (node.containsPoint(e.getX(), e.getY())) {
                            selectedNode = node;
                            return;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectedNode = null;
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedNode != null) {
                    selectedNode.x = e.getX() - selectedNode.width / 2;
                    selectedNode.y = e.getY() - selectedNode.height / 2;
                    repaint();
                }
            }
        });
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("Delete Node");
        deleteItem.addActionListener(e -> {
            if (selectedNode != null) {
                nodes.removeIf(n -> n == selectedNode || isDescendant(selectedNode, n));
                repaint();
            }
        });

        JMenuItem addChildItem = new JMenuItem("Add Child Node");
        addChildItem.addActionListener(e -> {
            if (selectedNode != null) {
                String childText = JOptionPane.showInputDialog("Child Node Text:");
                if (childText != null && !childText.trim().isEmpty()) {
                    MindMapNode child = new MindMapNode(childText, selectedNode.x + selectedNode.width + 10, selectedNode.y + selectedNode.height + 10, Color.LIGHT_GRAY);
                    selectedNode.addChild(child);
                    nodes.add(child);
                    repaint();
                }
            }
        });

        JMenuItem changeColorItem = new JMenuItem("Change Color");
        changeColorItem.addActionListener(e -> {
            if (selectedNode != null) {
                Color newColor = JColorChooser.showDialog(null, "Choose a color", selectedNode.color);
                if (newColor != null) {
                    selectedNode.setColor(newColor);
                    repaint();
                }
            }
        });
        JMenuItem exportToPNGItem = new JMenuItem("Export to PNG");
        exportToPNGItem.addActionListener(e -> exportToPNG());

        JMenuItem exportToXMLItem = new JMenuItem("Export to XML");
        exportToXMLItem.addActionListener(e -> exportToXML());


        menu.add(addChildItem);
        menu.add(exportToXMLItem);
        menu.add(deleteItem);
        menu.add(changeColorItem);
        menu.add(exportToPNGItem);

        return menu;
    }

    boolean isDescendant(MindMapNode parent, MindMapNode potentialDescendant) {
        if (parent.children.contains(potentialDescendant)) {
            return true;
        }
        for (MindMapNode child : parent.children) {
            if (isDescendant(child, potentialDescendant)) {
                return true;
            }
        }
        return false;
    }

    void addNode(MindMapNode node) {
        nodes.add(node);
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHints(rh);

        // Before drawing, let's ensure we're only drawing connections for existing nodes
        for (MindMapNode node : nodes) {
            drawNode(g2d, node);
        }
        for (MindMapNode node : nodes) {
            for (MindMapNode child : node.children) {
                if (nodes.contains(child)) { // Draw connection only if the child is in the active nodes list
                    drawConnection(g2d, node.x + node.width / 2, node.y + node.height / 2, child.x + child.width / 2, child.y + child.height / 2);
                }
            }
        }
    }

    private void drawNode(Graphics2D g2d, MindMapNode node) {
        Ellipse2D shape = new Ellipse2D.Double(node.x, node.y, node.width, node.height);
        g2d.setColor(node.color);
        g2d.fill(shape);
        g2d.setColor(Color.BLACK);
        g2d.draw(shape);
        drawCenteredText(g2d, node.text, node.x, node.y, node.width, node.height);
    }

    private void drawConnection(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2);
    }

    private void drawCenteredText(Graphics2D g2d, String text, int x, int y, int width, int height) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }
    private void exportToXML() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("MindMap");
            doc.appendChild(rootElement);

            for (MindMapNode node : nodes) {
                Element nodeElement = doc.createElement("Node");
                nodeElement.setAttribute("Text", node.text);
                nodeElement.setAttribute("X", String.valueOf(node.x));
                nodeElement.setAttribute("Y", String.valueOf(node.y));
                nodeElement.setAttribute("Color", Integer.toHexString(node.color.getRGB() & 0xffffff)); // Mask to remove alpha bits
                rootElement.appendChild(nodeElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("MindMap.xml"));

            transformer.transform(source, result);
            JOptionPane.showMessageDialog(this, "Exported as XML successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (ParserConfigurationException | TransformerException pce) {
            pce.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export as XML.", "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToPNG() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        paint(g2);
        g2.dispose();
        try {
            ImageIO.write(image, "png", new File("MindMap.png"));
            JOptionPane.showMessageDialog(this, "Exported as PNG successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export as PNG.", "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

public class MindMapApp extends JFrame {
    public MindMapApp() {
        initUI();
    }

    private void initUI() {
        MindMapSurface surface = new MindMapSurface();
        add(surface);

        MindMapNode root = new MindMapNode("Root", 300, 200, Color.CYAN);
        surface.addNode(root);

        setTitle("Advanced Mind Map");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            MindMapApp ex = new MindMapApp();
            ex.setVisible(true);
        });
    }
}