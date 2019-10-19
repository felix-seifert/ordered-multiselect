package com.felixseifert.addons;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Tag("ordered-multiselect")
public class OrderedMultiselect<T> extends CustomField<List<T>> implements HasEnabled {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LoggerFactory.getLogger(OrderedMultiselect.class);

    private Select<T> select = new Select<>();

    private ItemLabelGenerator<T> itemLabelGenerator;

    private Button addButton = new Button(VaadinIcon.ENTER_ARROW.create());

    private Label descriptionLabel = new Label();

    private HorizontalLayout labelLayout = new HorizontalLayout();

    private List<Object> itemList = new ArrayList<>();

    private String buttonText = "";

    private int maxNumberItems = 0;

    private boolean numbersLeftOfText = false;

    public OrderedMultiselect() {
        super(Collections.emptyList());
        add(descriptionLabel);
        add(createSelect());
        add(labelLayout);
        labelLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        setPresentationValue(getEmptyValue());
        refreshLabels();
    }

    public OrderedMultiselect(String selectLabel) {
        this();
        setSelectLabel(selectLabel);
    }

    @Override
    protected List<T> generateModelValue() {
        return itemList.stream().filter(i -> !(i instanceof Icon)).map(i -> (T) i).collect(Collectors.toList());
    }

    @Override
    protected void setPresentationValue(List<T> ts) {
        itemList.clear();
        refreshLabels();
        ts.stream().forEach(this::addListener);
    }

    private HorizontalLayout createSelect() {

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setMargin(false);
        horizontalLayout.setPadding(false);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        horizontalLayout.add(select, addButton);

        select.addValueChangeListener(event -> enableButton(event.getValue() != null));

        addButton.setEnabled(false);
        addButton.setIconAfterText(true);
        addButton.addClickListener(event -> addListener(select.getValue()));

        return horizontalLayout;
    }

    private void addListener(T toAdd) {
        if(toAdd == null) return;
        if(!itemList.isEmpty()) {
            itemList.add(createChangeIcon());
        }
        itemList.add(toAdd);
        refreshLabels();
        enableButton(false);
    }

    private void removeListener(String text) {
        if(itemList.isEmpty() || StringUtils.isBlank(text)) return;

        int numberInParentheses;
        try {
            String numberText;
            if(numbersLeftOfText) {
                numberText = text.split("\\)\\s")[0];
                numberInParentheses = Integer.parseInt(numberText.substring(1));
            }
            else {
                numberText = text.split("\\s\\(")[1];
                numberInParentheses = Integer.parseInt(numberText.substring(0, numberText.length() - 1));
            }
        } catch (IndexOutOfBoundsException | PatternSyntaxException | NumberFormatException e) {
            LOGGER.error("Button texts are invalid (labelLayout modified or Roles have wrong name).");
            return;
        }

        int index = (2 * numberInParentheses) - 2;
        itemList.remove(index);

        enableButton(true);

        if(itemList.isEmpty()) {
            refreshLabels();
            return;
        }

        if(index < itemList.size()) {
            itemList.remove(index);
            refreshLabels();
            return;
        }

        itemList.remove(index - 1);
        refreshLabels();
    }

    private void refreshLabels() {
        labelLayout.removeAll();
        int numberOfTs = 0;
        for(int i = 0; i < itemList.size(); i++) {
            Object item = itemList.get(i);
            if(item instanceof Icon) {
                ((Icon) item).setId("" + i);
                labelLayout.add((Icon) item);
                continue;
            }
            numberOfTs++;
            Button button = new Button();
            if(numbersLeftOfText) {
                button.setText(String.format("(%d) %s", numberOfTs, itemLabelGenerator.apply((T) item)));
            }
            else {
                button.setText(String.format("%s (%d)", itemLabelGenerator.apply((T) item), numberOfTs));
            }
            button.addClickListener(event -> removeListener(event.getSource().getText()));
            labelLayout.add(button);
        }
        updateValue();
    }

    private void enableButton(boolean isValueSelected) {
        boolean maxReached = maxNumberItems > 0 && itemList.size() >= (2 * maxNumberItems - 1);
        addButton.setEnabled(isValueSelected && !maxReached);

        if(maxReached) {
            addButton.setText("No items could be added anymore.");
            return;
        }
        addButton.setText(buttonText);
    }

    private Icon createChangeIcon() {
        Icon icon = new Icon(VaadinIcon.EXCHANGE);
        setIconStyle(icon, isEnabled());
        icon.setSize("18px");
        icon.addClickListener(event -> exchangeListener(icon.getId().orElse(null)));
        return icon;
    }

    private void setIconStyle(Icon icon, boolean enabled) {
        if(enabled) {
            icon.getStyle().set("cursor", "pointer").set("color", "var(--lumo-secondary-text-color)");
            return;
        }
        icon.getStyle().set("cursor", "default").set("color", "var(--lumo-disabled-text-color)");

    }

    private void exchangeListener(String id) {
        if(itemList.isEmpty() || StringUtils.isBlank(id)) return;

        int number;
        try {
            number = Integer.parseInt(id);
        }
        catch(NumberFormatException e) {
            LOGGER.error("ID is invalid (id modified).");
            return;
        }

        Collections.swap(itemList, number - 1, number + 1);
        refreshLabels();
        enableButton(true);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    @Override
    public void setLabel(String label) {
        setSelectLabel(label);
    }

    @Override
    public String getLabel() {
        return getSelectLabel();
    }

    public void setSelectLabel(String label) {
        select.setLabel(label);
    }

    public String getSelectLabel() {
        return select.getLabel();
    }

    public void setSelectWidth(String width) {
        select.setWidth(width);
    }

    public String getSelectWidth() {
        return select.getWidth();
    }

    public void setButtonText(String text) {
        buttonText = text;
        addButton.setText(text);
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonWidth(String width) {
        addButton.setWidth(width);
    }

    public String getButtonWidth() {
        return addButton.getWidth();
    }

    public void setItems(Collection<T> items) {
        select.setItems(items);
    }

    public void setItems(T... items) {
        this.setItems(Arrays.asList(items));
    }

    public void setItems(Stream<T> streamOfItems) {
        this.setItems(streamOfItems.collect(Collectors.toList()));
    }

    public void setItemLabelGenerator(ItemLabelGenerator<T> itemLabelGenerator) {
        this.itemLabelGenerator = itemLabelGenerator;
        select.setItemLabelGenerator(itemLabelGenerator);
    }

    public void setMaxNumberItems(Integer max) {
        if(max == null) max = 0;
        this.maxNumberItems = max;
    }

    public void setNumbersLeftOfText(Boolean numbersLeftOfText) {
        if(numbersLeftOfText == null) numbersLeftOfText = false;
        this.numbersLeftOfText = numbersLeftOfText;
        refreshLabels();
    }

    @Override
    public void setValue(List<T> value) {
        itemList.clear();
        refreshLabels();
        value.forEach(this::addListener);
    }

    @Override
    public List<T> getValue() {
        return itemList.stream().filter(item -> !(item instanceof Icon))
                .map(item -> (T) item).collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return itemList.isEmpty();
    }

    @Override
    public void setEnabled(boolean enabled) {
        select.setEnabled(enabled);
        enableButton(enabled);
        labelLayout.setEnabled(enabled);

        if(enabled) {
            labelLayout.getChildren().filter(component -> component instanceof Icon)
                    .forEach(icon -> setIconStyle((Icon) icon, true));
            return;
        }
        labelLayout.getChildren().filter(component -> component instanceof Icon)
                .forEach(icon -> setIconStyle((Icon) icon, false));
    }

    @Override
    public boolean isEnabled() {
        return select.isEnabled();
    }

    @Override
    public void clear() {
        itemList.clear();
        refreshLabels();
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        select.setErrorMessage(errorMessage);
    }

    @Override
    public String getErrorMessage() {
        return select.getErrorMessage();
    }

    @Override
    public List<T> getEmptyValue() {
        return super.getEmptyValue();
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        select.setRequiredIndicatorVisible(requiredIndicatorVisible);
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return select.isRequiredIndicatorVisible();
    }

    @Override
    public void setInvalid(boolean invalid) {
        select.setInvalid(invalid);
        getElement().setProperty("invalid", invalid);
    }

    @Override
    public boolean isInvalid() {
        return select.isInvalid();
    }

}
