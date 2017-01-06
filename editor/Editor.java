package editor;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;

/**
 * A JavaFX application that displays the letter the user has typed most recently in the center of
 * the window. Pressing the up and down arrows causes the font size to increase and decrease,
 * respectively.
 */
public class Editor extends Application {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 500;
    private static final int STARTING_FONT_SIZE = 20;
    private int fontSize = STARTING_FONT_SIZE;
    private String fontName = "Verdana";
    private static int margin = 5;
    private final Rectangle cursor;
    private final DoublyLinkedList textFile;
    private DoublyLinkedList cursorPosition;
    private LinkedList<ActionDone> undo;
    private LinkedList<ActionDone> redo;
    private int windowWidth = 500;
    private int windowHeight = 500;
    private Text sampleText;
    private int textHeight;
    private Map<Integer, DoublyLinkedList> lineStart;
    private int currentLine;
    private int numLines;
    //0 means the top of the text is shown. Any positive number means the text
    //is moved UP by that many number of pixels.
    private int scrollingInt = 0;
    private ScrollBar scrollBar;
    private int textMaxWidth;


    public Editor() {
        sampleText = new Text("x");
        sampleText.setFont(Font.font(fontName, fontSize));
        textHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());
        cursor = new Rectangle(1, textHeight);
        textFile = new DoublyLinkedList();
        undo = new LinkedList<ActionDone>();
        redo = new LinkedList<ActionDone>();
        cursorPosition = textFile;
        currentLine = 0;
        numLines = 1;
        lineStart = new HashMap<Integer, DoublyLinkedList>();
        scrollBar = new ScrollBar();
    }

    /** An EventHandler to handle keys that get pressed. */
    private class KeyEventHandler implements EventHandler<KeyEvent> {
        private final Group root;

        KeyEventHandler(Group root) {
            this.root = root;
            // All new Nodes need to be added to the root in order to be displayed.
            formatText(textFile);
            addTextToRoot();
            snappingCursor();
            renderScreenImage(textFile);
        }

        //Used in text wrapping. Returns null if the word doesn't need to
        //be moved onto the next line. Returns the beginning of the word
        //otherwise.
        private DoublyLinkedList wordBeginning(DoublyLinkedList breakPoint) {
            if (breakPoint.parent == null) {
                return null;
            } else if (breakPoint.head.getText().charAt(0) == ' ') {
                return breakPoint.tail;
            } else if (breakPoint.head.getX() == margin) {
                return null;
            } else {
                return wordBeginning(breakPoint.parent);
            }
        }

        //Adds all the text in textFile to the textRoot. Puts cursor position
        //at the end of the file.
        private void addTextToRoot() {
            DoublyLinkedList iter = textFile.tail;
            DoublyLinkedList trailer = textFile;
            while (iter != null) {
                root.getChildren().add(iter.head);
                trailer = iter;
                iter = iter.tail;
            }
            cursorPosition = trailer;
            setCursorToAfterNode();
        }

        private void redoAction(LinkedList<ActionDone> lst1, LinkedList<ActionDone> lst2) {
            if (lst1.size() > 0) {
                ActionDone ad = lst1.removeLast();
                ad.undoAction();
                ad.switchAction();
                lst2.add(ad);
                if (ad.action == ad.DELETE) {
                    root.getChildren().remove(ad.node.head);
                    cursorPosition = ad.node.parent;
                } else if (ad.action == ad.ADD) {
                    root.getChildren().add(ad.node.head);
                    cursorPosition = ad.node;
                }
            }
        }


        @Override
        public void handle(KeyEvent keyEvent) {
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.
                if (!keyEvent.isShortcutDown()) {
                    String characterTyped = keyEvent.getCharacter();
                    if (characterTyped.length() > 0) {
                        redo.clear();
                        char character = characterTyped.charAt(0);
                        Text newChar = null;
                        if (character != 8) {
                            if (character == '\r') {
                                newChar = new Text("\n");
                            } else {
                                newChar = new Text(characterTyped);
                            }
                            if (undo.size() > 100) {
                                undo.removeFirst();
                            }
                            cursorPosition.add(newChar);
                            cursorPosition = cursorPosition.tail;
                            root.getChildren().add(newChar);
                            undo.add(new ActionDone(cursorPosition, "add"));
                            //renderScreenImage(cursorPosition);
                            renderScreenImage(textFile);
                            snappingCursor();
                            renderScreenImage(textFile);


                        } else {
                            if (cursorPosition.parent != null) {
                                DoublyLinkedList temp = cursorPosition;
                                undo.add(new ActionDone(cursorPosition, "delete"));
                                root.getChildren().remove(cursorPosition.head);
                                cursorPosition.remove();
                                cursorPosition = cursorPosition.parent;
                                int cursorY = (int) cursor.getY();
                                int oldScrollingInt = scrollingInt;
                                double cursorLine = 0;
                                double nextCharLine = 0;
                                //Fast method - Doesn't push up words after space if the 
                                //word before it fits on the previous line 
                                //renderScreenImage(cursorPosition.tail);
                                renderScreenImage(textFile);
                                if (cursorPosition.tail != null && cursorPosition.tail.head.getText().charAt(0) != '\n') {
                                    cursorLine = (cursorPosition.head.getY() + scrollingInt) / textHeight;
                                    nextCharLine = (cursorPosition.tail.head.getY() + scrollingInt) / textHeight;
                                }
                                if (cursorLine != nextCharLine && cursorLine != (cursorY + scrollingInt) / textHeight) {
                                    cursor.setY(cursorY);
                                    snappingCursor();
                                    renderScreenImage(textFile);
                                    cursor.setX(margin);
                                    cursor.setY(cursorY + oldScrollingInt - scrollingInt);
                                } else {
                                    snappingCursor();
                                    renderScreenImage(textFile);
                                }
                            }
                        }
                        keyEvent.consume();
                    }
                }
            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).
                KeyCode code = keyEvent.getCode();
                if (code == KeyCode.LEFT) {
                    if (cursorPosition.parent != null) {
                        int oldLine = currentLine;
                        cursorPosition = cursorPosition.parent;
                        int cursorX = (int) cursor.getX();
                        int cursorY = (int) cursor.getY();
                        int oldScrollingInt = scrollingInt;
                        setCursorToAfterNode();
                        int newLine = ((int) cursorPosition.head.getY() + scrollingInt) / textHeight;
                        if (oldLine == newLine || cursorX == margin) {
                            snappingCursor();
                            renderScreenImage(textFile);
                        } else {
                            cursor.setY(cursorY);
                            snappingCursor();
                            formatText(textFile);
                            currentLine = oldLine;
                            cursor.setX(margin);
                            cursor.setY(cursorY + oldScrollingInt - scrollingInt);
                        }

                    }
                } else if (code == KeyCode.RIGHT) {
                    if (cursorPosition.tail != null) {
                        cursorPosition = cursorPosition.tail;
                        renderScreenImage(textFile);
                        snappingCursor();
                        renderScreenImage(textFile);
                    }
                } else if (code == KeyCode.UP) {
                    if (currentLine > 0) {
                        arrowKeyUpOperations();
                    }
                } else if (code == KeyCode.DOWN) {
                    if (currentLine < numLines - 1) {
                        arrowKeyDownOperations();
                    }
                }
                //Check CTRL + KEY functions
                if (keyEvent.isShortcutDown()) {
                    if (code == KeyCode.S) {
                        saveFile();
                    } else if (code == KeyCode.EQUALS || code == KeyCode.PLUS) {
                        fontSize += 4;
                        sampleText.setFont(Font.font(fontName, fontSize));
                        textHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());
                        cursor.setHeight(textHeight);
                        renderScreenImage(textFile);
                    } else if (code == KeyCode.MINUS) {
                        fontSize = Math.max(fontSize - 4, 4);
                        sampleText.setFont(Font.font(fontName, fontSize));
                        textHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());
                        cursor.setHeight(textHeight);
                        renderScreenImage(textFile);
                    } else if (code == KeyCode.Z) {
                        redoAction(undo, redo);
                        renderScreenImage(textFile);
                        snappingCursor();
                        renderScreenImage(textFile);
                    } else if (code == KeyCode.Y) {
                        redoAction(redo, undo);
                        renderScreenImage(textFile);
                        snappingCursor();
                        renderScreenImage(textFile);
                    } else if (code == KeyCode.P) {
                        System.out.println("Cursor xPos: " + cursor.getX() + 
                                            " yPos: " + cursor.getY());
                    }
                }
            }
        }
    }
    private boolean ambiguousCursorPosition() {
        if (cursorPosition.tail != null) {
            double cursorLine = (cursorPosition.head.getY() + scrollingInt) / textHeight;
            double nextCharLine = (cursorPosition.tail.head.getY() + scrollingInt) / textHeight;
            if (cursorLine != nextCharLine) {
                return true;
            }
        }
        return false;
    }
    private void checkLeftArrowKeyCondition(int tempLine, int oldX) {
        if (currentLine != tempLine && oldX != margin) {
            cursor.setX(margin);
            cursor.setY(cursor.getY() + textHeight);
        }
    }

    //Updates the scrollingInt variable which is used in format
    //Text to move all the text up or down
    //Returns true if it did something, false if it did nothing.
    private boolean snappingCursor() {
        if (cursor.getY() < 0) {
            scrollingInt += (int) cursor.getY();
            scrollBar.setValue(scrollingInt + windowHeight);
            return true;
        } else if (cursor.getY() + cursor.getHeight() > windowHeight) {
            scrollingInt += (int) cursor.getY() + cursor.getHeight() - windowHeight;
            scrollBar.setValue(scrollingInt + windowHeight);
            return true;
        }
        return false;
    }
    private void formatText(DoublyLinkedList text) {
        DoublyLinkedList iter = text;
        while (iter != null) {
            if (iter.parent == null) {
                iter.head.setY(-scrollingInt);
                lineStart.put(0, iter);
                currentLine = 1;
                numLines = 1;
            } else {
                Text t = iter.head;
                Text parentText = iter.parent.head;
                t.setTextOrigin(VPos.TOP);
                t.setFont(Font.font(fontName, fontSize));
                int posY = (int) parentText.getY();
                int posX = margin;
                if (t.getText().charAt(0) == '\n') {
                    posY += textHeight;
                } else {
                    posX = (int) (parentText.getX() + 
                            Math.round(parentText.getLayoutBounds().getWidth()));
                }
                //Wraps text around if character does not fit and is not a space
                if (posX + Math.round(t.getLayoutBounds().getWidth()) > (textMaxWidth - margin) && 
                    t.getText().charAt(0) != ' ') {
                    DoublyLinkedList word = wordBeginning(iter.parent);
                    posX = margin;
                    posY += textHeight;
                    if (word != null) {
                        //Adds beginning of this word to lineStart map since it'll be
                        //on a new line
                        lineStart.put(currentLine, word);
                        currentLine += 1;
                        numLines += 1;
                        scrollBar.setMax(Math.max(scrollingInt + windowHeight, numLines * textHeight));
                        word.head.setX(posX);
                        word.head.setY(posY);
                        formatText(word.tail);
                        break;
                    }
                }
                t.setX(posX);
                t.setY(posY);

                //Adds the beginning of each line to the lineStart map
                if (posX == margin) {
                    int lineNum = (((int) t.getY() + scrollingInt) / textHeight);
                    scrollBar.setMax(Math.max(scrollingInt + windowHeight, numLines * textHeight));
                    if (lineNum == currentLine) {
                        lineStart.put(currentLine, iter);
                        currentLine += 1;
                        numLines += 1;
                    }
                }
            }
            iter = iter.tail;
        }
    }

    private void arrowKeyUpOperations() {
        int expectedLine = currentLine - 1;
        currentLine -= 1;
        int workToBeDone = arrowKeyUpDownConditions();
        cursor.setY(cursor.getY() - textHeight);
        snappingCursor();
        renderScreenImage(textFile);
        currentLine = expectedLine;
        if (workToBeDone == 1) {
            cursor.setX(margin);
            cursor.setY(cursor.getY() + textHeight);
        }
    }
    private void arrowKeyDownOperations() {
        int expectedLine = currentLine + 1;
        currentLine += 1;
        int workToBeDone = arrowKeyUpDownConditions();
        cursor.setY(cursor.getY() + textHeight);
        snappingCursor();
        renderScreenImage(textFile);
        currentLine = expectedLine;
        if (workToBeDone == 1) {
            cursor.setX(margin);
            cursor.setY(cursor.getY() + textHeight);
        }

    }
    //Assumes currentLine already incremented up or down by 1 due to arrow key
    //Sets cursorPosition to the node above/below that is closest to its current X postion
    //returns 0 if there is no work to be done after rendering
    //returns 1 if cursor position needs to be adjusted after rendering
    private int arrowKeyUpDownConditions() {
        int expectedLine = currentLine;
        DoublyLinkedList iter = lineStart.get(expectedLine);
        DoublyLinkedList trailer = iter;
        int cursorX = (int) cursor.getX();
        int cursorY = (int) cursor.getY();
        int frontX = 0;
        int backX = 0;
        if (cursorX == margin) {
            cursorPosition = iter;
            if (cursorPosition.head.getX() + 
                cursorPosition.head.getLayoutBounds().getWidth() != margin) {
                cursorPosition = cursorPosition.parent;
                return 1;
            }
        } else {
            while (iter != null) {
                if ((iter.head.getY() + scrollingInt) / textHeight != expectedLine) {
                    cursorPosition = trailer;
                    return 0;
                } else {
                    frontX = (int) iter.head.getX() + (int) iter.head.getLayoutBounds().getWidth();
                    backX = (int) trailer.head.getX() + (int) trailer.head.getLayoutBounds().getWidth();
                    if (frontX > cursorX) {
                        break;
                    }
                    trailer = iter;
                    iter = iter.tail;
                }
            }
            if (iter == null) {
                cursorPosition = trailer;
            } else {
                int frontDistance = Math.abs(frontX - cursorX);
                int backDistance = Math.abs(backX - cursorX);
                if (frontDistance < backDistance) {
                    cursorPosition = iter;
                } else {
                    cursorPosition = trailer;
                }
            }
        }
        return 0;
    }

    private void setCursorToAfterNode() {
        cursor.setX(cursorPosition.head.getX() + 
           Math.round(cursorPosition.head.getLayoutBounds().getWidth()));
        cursor.setX(Math.min(cursor.getX(), textMaxWidth - margin));
        cursor.setY(cursorPosition.head.getY());
        currentLine = ((int) cursor.getY() + scrollingInt) / textHeight;
    }

    private void renderScreenImage(DoublyLinkedList node) {
        formatText(node);
        setCursorToAfterNode();
    }

    //Used in text wrapping. Returns null if the word doesn't need to
    //be moved onto the next line. Returns the beginning of the word
    //otherwise.
    private DoublyLinkedList wordBeginning(DoublyLinkedList breakPoint) {
        if (breakPoint.parent == null) {
            return null;
        } else if (breakPoint.head.getText().charAt(0) == ' ') {
            return breakPoint.tail;
        } else if (breakPoint.head.getX() == margin) {
            return null;
        } else {
            return wordBeginning(breakPoint.parent);
        }
    }


    private void saveFile() {
        try {
            FileWriter writer = new FileWriter(getArgs().get(0));
            DoublyLinkedList iter = textFile.tail;
            while (iter != null) {
                writer.write(iter.head.getText());
                iter = iter.tail;
            }
            writer.close();
            System.out.println("Save Successful!");
        } catch (IOException e) {
            System.out.println("Tried to write file but received directory name");
        }
    }

    /** An EventHandler to handle changing the color of the rectangle. */
    private class Cursor implements EventHandler<ActionEvent> {
        private int indexofColor = 0;
        private Color[] colors = {Color.BLACK, Color.WHITE};

        @Override
        public void handle(ActionEvent event) {
            cursor.setFill(colors[indexofColor]);
            indexofColor = (indexofColor + 1) % colors.length;
        }
    }


    /** Makes the cursor blink periodically. */
    public void makeCursorBlink() {
        // Create a Timeline that will call the "handle" function of Cursor
        // every 0.5 second.
        final Timeline timeline = new Timeline();
        // The cursor should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        Cursor cursor = new Cursor();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), cursor);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private List<String> getArgs() {
        List<String> args = getParameters().getRaw();
        if (args.size() < 1) {
            System.out.println("Please provide a file name to open or create");
            System.exit(1);
        }
        return args;
    }

    private void readFile() {
        List<String> args = getArgs();
        File file = new File(args.get(0));
        DoublyLinkedList lastNode = textFile;
        try {
            if (!file.exists()) {
                return;
            }
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            for (int character = br.read(); character != -1; character = br.read()) {
                lastNode.add(new Text(String.valueOf((char) character)));
                lastNode = lastNode.tail;
            }
            br.close();
            return;
        } catch (FileNotFoundException e) {
            System.out.println("Tried to read non-existent file");
        } catch (IOException e) {
            System.out.println("Error when trying to read file");
        }
        System.exit(1);
    }

    private void printFile() {
        DoublyLinkedList iter = textFile;
        while (iter != null) {
            System.out.print(iter);
            iter = iter.tail;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        readFile();
        // Create a Node that will be the parent of all things displayed on the screen.
        Group root = new Group();
        Group textRoot = new Group();
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, Color.WHITE);
        root.getChildren().add(textRoot);

        //adds a ScrollBar to the right of the window
        root.getChildren().add(scrollBar);
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setPrefHeight(windowHeight);
        textMaxWidth = windowWidth - (int) scrollBar.getWidth();
        scrollBar.setLayoutX(windowWidth - scrollBar.getWidth());
        scrollBar.setUnitIncrement(textHeight);
        scrollBar.setBlockIncrement(textHeight);
        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue,
                Number oldValue, Number newValue) {
                int cursorX = (int) cursor.getX();
                int cursorY = (int) cursor.getY();
                int oldScrollingInt = scrollingInt;
                scrollingInt = (int) (double) newValue - windowHeight;
                scrollBar.setMax(Math.max(scrollingInt + windowHeight, numLines * textHeight));
                scrollBar.setVisibleAmount((windowHeight / scrollBar.getMax()) * 
                                        (scrollBar.getMax() - scrollBar.getMin()));
                renderScreenImage(textFile);
                cursor.setX(cursorX);
                cursor.setY(cursorY + oldScrollingInt - scrollingInt);
            }
        });
        //javaFX bug that sets width to 20 by default but visually it is 10, 
        //which leaves a white border of pixels to the right of the scrollbar on creation.
        //Resizing fixes this
        scrollBar.setMin(windowHeight);
        scrollBar.setVisibleAmount(windowHeight);
        scrollBar.setMax(windowHeight);
        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler = new KeyEventHandler(textRoot);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);

        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                int frontX = 0;
                int backX = 0;
                boolean cursorAtStart = false;
                double marginRange = 0;
                int mouseLine = (int) (mouseEvent.getY() + scrollingInt) / textHeight;                if (mouseLine >= numLines - 1) {
                    mouseLine = numLines - 1;
                }
                DoublyLinkedList iter = lineStart.get(mouseLine);
                DoublyLinkedList trailer = iter;
                if (iter.parent != null && iter.head.getText().charAt(0) != '\n') {
                    marginRange = margin + iter.head.getLayoutBounds().getWidth() / 2;
                }
                if (mouseEvent.getX() <= marginRange && iter.parent != null && iter.head.getText().charAt(0) != '\n') {
                    cursorPosition = iter.parent;
                    cursor.setX(margin);
                    cursor.setY(iter.head.getY());
                    return;
                } else {
                    while (iter != null) {
                        if ((iter.head.getY() + scrollingInt) / textHeight != mouseLine) {
                            cursorPosition = trailer;
                            break;
                        } else {
                            frontX = (int) iter.head.getX() + (int) iter.head.getLayoutBounds().getWidth();
                            backX = (int) trailer.head.getX() + (int) trailer.head.getLayoutBounds().getWidth();
                            if (frontX > mouseEvent.getX()) {
                                double frontDistance = Math.abs(frontX - mouseEvent.getX());
                                double backDistance = Math.abs(backX - mouseEvent.getX());
                                if (frontDistance < backDistance) {
                                    cursorPosition = iter;
                                } else {
                                    cursorPosition = trailer;
                                }
                                break;
                            }
                            trailer = iter;
                            iter = iter.tail;
                        }
                    }
                    if (iter == null) {
                        cursorPosition = trailer;
                    }
                    setCursorToAfterNode();
                }
            }
        });

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, 
                Number oldWidth, Number newWidth) {
                windowWidth = (int) (double) newWidth;
                scrollBar.setLayoutX(windowWidth - scrollBar.getWidth());
                textMaxWidth = windowWidth - (int) scrollBar.getWidth();
                renderScreenImage(textFile);
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, 
                Number oldHeight, Number newHeight) {
                windowHeight = (int) (double) newHeight;
                scrollBar.setPrefHeight(windowHeight);
                renderScreenImage(textFile);
            }
        });
        root.getChildren().add(cursor);
        makeCursorBlink();

        primaryStage.setTitle("Editor");

        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setWidth(windowWidth);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
