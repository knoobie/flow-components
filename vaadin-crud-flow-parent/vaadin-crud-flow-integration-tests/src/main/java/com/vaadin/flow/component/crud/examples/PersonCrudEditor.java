package com.vaadin.flow.component.crud.examples;

import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Element;

public class PersonCrudEditor implements CrudEditor<Person> {

    private final TextField firstNameField = new TextField("First name");
    private final TextField lastNameField = new TextField("Last name");

    private final FormLayout view = new FormLayout();

    private Person editableItem;
    private Binder<Person> binder;

    PersonCrudEditor() {
        view.add(firstNameField, lastNameField);
    }

    @Override
    public Person getItem() {
        return editableItem;
    }

    @Override
    public void setItem(Person item) {
        binder = new Binder<>(Person.class);
        binder.bind(firstNameField, Person::getFirstName, Person::setFirstName);
        binder.bind(lastNameField, Person::getLastName, Person::setLastName);

        editableItem = copyOf(item);
        binder.setBean(editableItem);
    }

    private Person copyOf(Person item) {
        final Person copy = new Person();
        copy.setId(item.getId());
        copy.setFirstName(item.getFirstName());
        copy.setLastName(item.getLastName());
        return copy;
    }

    @Override
    public void clear() {
        if (binder != null) {
            binder.removeBinding("firstName");
            binder.removeBinding("lastName");
            binder.removeBean();
            binder = null;
        }

        editableItem = null;

        firstNameField.clear();
        lastNameField.clear();
    }

    @Override
    public boolean isValid() {
        return binder != null && binder.isValid();
    }

    @Override
    public boolean isDirty() {
        return binder != null && binder.hasChanges();
    }

    @Override
    public Element getView() {
        return view.getElement();
    }
}
