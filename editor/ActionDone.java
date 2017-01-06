package editor;

public class ActionDone {
	DoublyLinkedList parent;
	DoublyLinkedList node;
	String action = "";
	String DELETE = "delete";
	String ADD = "add";

	//action must be "add" or "delete"
	public ActionDone(DoublyLinkedList node, String action) {
		this.node = node;
		this.parent = node.parent;
		this.action = action;
	}

	public void switchAction() {
		if (action.equals(ADD)) {
			action = DELETE;
		} else if (action.equals(DELETE)) {
			action = ADD;
		}
	}

	public void undoAction() {
		if (action == ADD) {
			remove();
		} else if (action == DELETE) {
			add();
		}
	}
	//adds node back to parent
	private void add() {
		parent.add(node);
	}
	//removes node from its DoublyLinkedList
	private void remove() {
		node.remove();
	}
}