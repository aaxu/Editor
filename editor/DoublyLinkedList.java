package editor;

import java.util.LinkedList;
import javafx.scene.text.Text;
import javafx.geometry.VPos;

public class DoublyLinkedList {
	public Text head;
	public DoublyLinkedList tail;
	public DoublyLinkedList parent;

	private DoublyLinkedList(Text t, DoublyLinkedList next) {
		head = t;
		tail = next;
		parent = null;
		if (tail != null) {
			tail.parent = this;
		}
	}

	public DoublyLinkedList() {
		head = new Text(5, 0, "");
		tail = null;
		parent = null;
	}

	public void add(Text t) {
		tail = new DoublyLinkedList(t, tail);
		tail.parent = this;
	}

	public void add(DoublyLinkedList node) {
		if (tail != null) {
			tail.parent = node;
		}
		node.tail = tail;
		tail = node;
		node.parent = this;
	}

	public void removePrev() {
		if (parent.parent != null) {
			this.parent = this.parent.parent;
			this.parent.tail = this;
		}
	}

	public void remove() {
		if (tail != null) {
			tail.parent = parent;
		}
		parent.tail = tail;
	}

	public void removeLast() {
		DoublyLinkedList trailer = this;
		DoublyLinkedList iter = tail;
		while (iter != null) {
			trailer = iter;
			iter = iter.tail;
		}
		trailer.parent.tail = null;
	}

}