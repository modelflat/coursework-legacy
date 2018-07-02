package com.github.modelflat.coursework.ui;

import com.github.modelflat.coursework.core.EvolvableParameter;
import com.github.modelflat.coursework.util.NoSuchResourceException;
import com.github.modelflat.coursework.util.Util;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

/**
 * Created on 09.05.2017.
 */
public class ParamCustomizationPane extends AnchorPane {
    private final EvolvableParameter param;
    private final EvolvableParameter prevState;

    @FXML
    private Button revertButton;
    @FXML
    private Button applyButton;
    @FXML
    private TextField upperBoundText;
    @FXML
    private TextField lowerBoundText;
    @FXML
    private ToggleButton evolveToggle;
    @FXML
    private Slider valueSlider;
    @FXML
    private TextField valueText;

    public ParamCustomizationPane(EvolvableParameter param) throws NoSuchResourceException {
        this.param = param;
        this.prevState = new EvolvableParameter();
        Util.loadCustomControl("fxml/param_customization_pane.fxml", this);
    }

    public void initialize() {
        prevState.copy(param);

        this.lowerBoundText.setText("" + param.getLower());
        this.upperBoundText.setText("" + param.getUpper());
        this.valueText.setText("" + param.getValue());

        this.valueSlider.setMin(param.getLower());
        this.valueSlider.setMax(param.getUpper());

        this.valueSlider.setValue(param.getValue());

        this.evolveToggle.setSelected(param.evolving());

        applyButton.setOnMouseClicked(this::applyButtonClicked);
        revertButton.setOnMouseClicked(this::revertButtonClicked);
        evolveToggle.setOnMouseClicked(this::evolveToggleClicked);

        this.lowerBoundText.textProperty().bindBidirectional(valueSlider.minProperty(), new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return "" + object.doubleValue();
            }

            @Override
            public Number fromString(String string) {
                double lower = valueSlider.getMin();
                if (string == null) {
                    return lower;
                }
                try {
                    lower = Double.valueOf(string);
                    lowerBoundText.setStyle("-fx-text-fill: black");
                    param.setLower(lower);
                } catch (NumberFormatException e) {
                    lowerBoundText.setStyle("-fx-text-fill: red");
                }
                return lower;
            }
        });
        this.upperBoundText.textProperty().bindBidirectional(valueSlider.maxProperty(), new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return "" + object.doubleValue();
            }

            @Override
            public Number fromString(String string) {
                double upper = valueSlider.getMax();
                if (string == null) {
                    return upper;
                }
                try {
                    upper = Double.valueOf(string);
                    upperBoundText.setStyle("-fx-text-fill: black");
                    param.setUpper(upper);
                } catch (NumberFormatException e) {
                    upperBoundText.setStyle("-fx-text-fill: red");
                }
                return upper;
            }
        });
        this.valueText.textProperty().bindBidirectional(valueSlider.valueProperty(), new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return "" + object.doubleValue();
            }

            @Override
            public Number fromString(String string) {
                double value = valueSlider.getValue();
                if (string == null) {
                    return value;
                }
                try {
                    value = Double.valueOf(string);
                    valueText.setStyle("-fx-text-fill: black");
                    param.setValue(value);
                } catch (NumberFormatException e) {
                    valueText.setStyle("-fx-text-fill: red");
                }
                return value;
            }
        });
    }

    public void applyButtonClicked(MouseEvent event) {
        prevState.copy(param);
    }

    public void revertButtonClicked(MouseEvent event) {
        param.copy(prevState);
    }

    public void evolveToggleClicked(MouseEvent event) {
        param.setDoEvolve(evolveToggle.isSelected());
    }
}
