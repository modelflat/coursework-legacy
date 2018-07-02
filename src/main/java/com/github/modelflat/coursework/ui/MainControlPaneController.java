package com.github.modelflat.coursework.ui;

import com.github.modelflat.coursework.App;
import com.github.modelflat.coursework.core.EvolvableParameter;
import com.github.modelflat.coursework.core.MyGLCanvasWrapper;
import com.github.modelflat.coursework.util.NoSuchResourceException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created on 06.05.2017.
 */
public class MainControlPaneController {

    @FXML
    private TextField colorTextBox;
    @FXML
    private Button saveImageButton;
    @FXML
    private TextField workItemsTextField;
    @FXML
    private TextField runCountTextField;
    @FXML
    private TextField iterCountTextField;
    @FXML
    private TextField skipCountTextField;

    @FXML
    private ToggleButton timeDirectionToggle;
    @FXML
    public ToggleButton tToggleButton;
    @FXML
    private Button applyRunSettingsButton;

    @FXML
    private VBox metaEvolutionPaneVBox;
    @FXML
    private VBox evolutionPaneVBox;
    @FXML
    private VBox boundsEvolutionPaneVBox;

    @FXML
    private CheckBox doRecomputeFractalCheckBox;
    @FXML
    private CheckBox doPostClearCheckBox;
    @FXML
    private CheckBox doCLClearCheckBox;

    // ==========================================================================
    // ==========================================================================
    @FXML
    private CheckBox doEvolveCheckBox;
    //
    @FXML
    private CheckBox doEvolveHCheckBox;
    @FXML
    private Button hEvolutionCustomizeButton;
    private Stage tEvolutionCustomizer;
    @FXML
    private Label hCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveCRealCheckBox;
    @FXML
    private Button cRealEvolutionCustomizeButton;
    private Stage cRealEvolutionCustomizer;
    @FXML
    private Label cRealCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveCImagCheckBox;
    @FXML
    private Button cImagEvolutionCustomizeButton;
    private Stage cImagEvolutionCustomizer;
    @FXML
    private Label cImagCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveBoundsCheckBox;
    @FXML
    private Slider boundsSlider;
    //
    @FXML
    private CheckBox doEvolveXMinCheckBox;
    @FXML
    private Button xMinEvolutionCustomizeButton;
    private Stage xMinEvolutionCustomizer;
    @FXML
    private Label xMinCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveXMaxCheckBox;
    @FXML
    private Button xMaxEvolutionCustomizeButton;
    private Stage xMaxEvolutionCustomizer;
    @FXML
    private Label xMaxCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveYMinCheckBox;
    @FXML
    private Button yMinEvolutionCustomizeButton;
    private Stage yMinEvolutionCustomizer;
    @FXML
    private Label yMinCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveYMaxCheckBox;
    @FXML
    private Button yMaxEvolutionCustomizeButton;
    private Stage yMaxEvolutionCustomizer;
    @FXML
    private Label yMaxCurrentStateLabel;
    // ==========================================================================
    // ==========================================================================

    @FXML
    private Slider hSlider;
    @FXML
    private Slider cRealSlider;
    @FXML
    private Slider cImagSlider;


    private Thread watcher;
    private boolean watcherRunning = true;
    private int pause = 150;

    public void initialize() {
        MyGLCanvasWrapper wrapper = App.getInstance().getWrapper();
        // set default values
        setRecomputeChecked(wrapper.doRecomputeFractal());
        doCLClearCheckBox.setSelected(wrapper.doCLClear());
        doPostClearCheckBox.setSelected(wrapper.doPostCLear());

        setEvolveChecked(wrapper.doEvolve());
        doEvolveHCheckBox.setSelected(wrapper.getH().evolving());
        doEvolveCRealCheckBox.setSelected(wrapper.getcReal().evolving());
        doEvolveCImagCheckBox.setSelected(wrapper.getcImag().evolving());
        setEvolveBoundsChecked(wrapper.doEvolveBounds());
        doEvolveXMinCheckBox.setSelected(wrapper.getMinX().evolving());
        doEvolveXMaxCheckBox.setSelected(wrapper.getMaxX().evolving());
        doEvolveYMinCheckBox.setSelected(wrapper.getMinY().evolving());
        doEvolveYMaxCheckBox.setSelected(wrapper.getMaxY().evolving());

        iterCountTextField.setText(wrapper.getNewtonKernelWrapper().getDefaultIterCount() + "");
        runCountTextField.setText(wrapper.getNewtonKernelWrapper().getDefaultRunCount() + "");
        workItemsTextField.setText(wrapper.getNewtonKernelWrapper().getDefaultWorkSize() + "");
        skipCountTextField.setText(wrapper.getNewtonKernelWrapper().getDefaultSkipCount() + "");

        boundsSlider.setMin(0.0);
        boundsSlider.setMax(10.0); // TODO replace magic
        boundsSlider.setValue(1.0);
        boundsSlider.setBlockIncrement(0.01);
        boundsSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();
            wrapper1.getMaxX().setValue(newValue.doubleValue());
            //scaleAndClamp(newValue.doubleValue(), wrapper1.getMaxX()));
            wrapper1.getMaxY().setValue(newValue.doubleValue());
            //scaleAndClamp(newValue.doubleValue(), wrapper1.getMaxY()));
            wrapper1.getMinY().setValue(-newValue.doubleValue());
            //scaleAndClamp(newValue.doubleValue(), wrapper1.getMinY()));
            wrapper1.getMinX().setValue(-newValue.doubleValue());
            //scaleAndClamp(newValue.doubleValue(), wrapper1.getMinX()));
        });

        hSlider.setMin(wrapper.getH().getLower());
        hSlider.setMax(wrapper.getH().getUpper());
        hSlider.setValue(wrapper.getH().getValue());
        hSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();
            wrapper1.getH().setValue(newValue.doubleValue());
        }));

        cRealSlider.setMin(wrapper.getcReal().getLower());
        cRealSlider.setMax(wrapper.getcReal().getUpper());
        cRealSlider.setValue(wrapper.getcReal().getValue());
        cRealSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();
            wrapper1.getcReal().setValue(newValue.doubleValue());
        });

        cImagSlider.setMin(wrapper.getcImag().getLower());
        cImagSlider.setMax(wrapper.getcImag().getUpper());
        cImagSlider.setValue(wrapper.getcImag().getValue());
        cImagSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();
            wrapper1.getcImag().setValue(newValue.doubleValue());
        });

        // set up parameter watcher thread
        // TODO this thread is needed to make mainloop more lightweight. probably it doesnt matter?
        watcher = new Thread(() -> {
            while (watcherRunning) {
                MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();

                String tText = String.format("t = %.12g", wrapper1.getH().getValue());
                String cRealText = String.format("cReal = %.12g", wrapper1.getcReal().getValue());
                String cImagText = String.format("cImag = %.12g", wrapper1.getcImag().getValue());
                String minXText = String.format("minX = %.12g", wrapper1.getMinX().getValue());
                String maxXText = String.format("maxX = %.12g", wrapper1.getMaxX().getValue());
                String minYText = String.format("minY = %.12g", wrapper1.getMinY().getValue());
                String maxYText = String.format("maxY = %.12g", wrapper1.getMaxY().getValue());

                Platform.runLater(() -> {
                    hCurrentStateLabel.setText(tText);
                    cRealCurrentStateLabel.setText(cRealText);
                    cImagCurrentStateLabel.setText(cImagText);
                    xMinCurrentStateLabel.setText(minXText);
                    xMaxCurrentStateLabel.setText(maxXText);
                    yMinCurrentStateLabel.setText(minYText);
                    yMaxCurrentStateLabel.setText(maxYText);
                });

                try {
                    Thread.sleep(pause);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "Parameter Watcher Thread");
        watcher.start();
    }

    private void setEvolveChecked(boolean state) {
        doEvolveCheckBox.setSelected(state);
        //evolutionPaneVBox.setDisable(!state);
    }

    private void setEvolveBoundsChecked(boolean state) {
        doEvolveBoundsCheckBox.setSelected(state);
        //boundsEvolutionPaneVBox.setDisable(!state);

        /// todo fix condition
        if ((state && !doEvolveXMaxCheckBox.isSelected() && !doEvolveXMinCheckBox.isSelected()
                && !doEvolveYMinCheckBox.isSelected() && !doEvolveYMaxCheckBox.isSelected()) ||
                (!state && doEvolveXMinCheckBox.isSelected() && doEvolveYMinCheckBox.isSelected()
                        && doEvolveXMaxCheckBox.isSelected() && doEvolveYMaxCheckBox.isSelected())) {
            MyGLCanvasWrapper wrapper = App.getInstance().getWrapper();
            doEvolveXMinCheckBox.setSelected(state);
            wrapper.getMinX().setDoEvolve(state);
            doEvolveXMaxCheckBox.setSelected(state);
            wrapper.getMaxX().setDoEvolve(state);
            doEvolveYMinCheckBox.setSelected(state);
            wrapper.getMinY().setDoEvolve(state);
            doEvolveYMaxCheckBox.setSelected(state);
            wrapper.getMaxY().setDoEvolve(state);
        }
    }

    private void setRecomputeChecked(boolean state) {
        doRecomputeFractalCheckBox.setSelected(state);
        metaEvolutionPaneVBox.setDisable(!state);
    }

    public void doPostClearCheckBoxClicked() {
        App.getInstance().getWrapper().setDoPostCLear(doPostClearCheckBox.isSelected());
    }

    public void doEvolveCheckBoxClicked() {
        setEvolveChecked(doEvolveCheckBox.isSelected());
        App.getInstance().getWrapper().setDoEvolve(doEvolveCheckBox.isSelected());
    }

    public void doCLClearCheckBoxClicked() {
        App.getInstance().getWrapper().setDoCLClear(doCLClearCheckBox.isSelected());
    }

    public void doEvolveHCheckBoxClicked() {
        App.getInstance().getWrapper().getH().setDoEvolve(doEvolveHCheckBox.isSelected());
    }

    public void hEvolutionCustomizeButtonClicked() {
        if (tEvolutionCustomizer == null) {
            tEvolutionCustomizer = createCustomizer("t", App.getInstance().getWrapper().getH());
        }
        tEvolutionCustomizer.show();
        tEvolutionCustomizer.toFront();
    }

    public void doEvolveCRealCheckBoxClicked() {
        App.getInstance().getWrapper().getcReal().setDoEvolve(doEvolveCRealCheckBox.isSelected());
    }

    public void cRealEvolutionCustomizeButtonClicked() {
        if (cRealEvolutionCustomizer == null) {
            cRealEvolutionCustomizer = createCustomizer("cReal", App.getInstance().getWrapper().getcReal());
        }
        cRealEvolutionCustomizer.show();
        cRealEvolutionCustomizer.toFront();
    }

    public void doEvolveCImagCheckBoxClicked() {
        App.getInstance().getWrapper().getcImag().setDoEvolve(doEvolveCImagCheckBox.isSelected());
    }

    public void cImagEvolutionCustomizeButtonClicked() {
        if (cImagEvolutionCustomizer == null) {
            cImagEvolutionCustomizer = createCustomizer("cImag", App.getInstance().getWrapper().getcImag());
        }
        cImagEvolutionCustomizer.show();
        cImagEvolutionCustomizer.toFront();
    }

    public void doEvolveBoundsCheckBoxClicked() {
        setEvolveBoundsChecked(doEvolveBoundsCheckBox.isSelected());
        App.getInstance().getWrapper().setDoEvolveBounds(doEvolveBoundsCheckBox.isSelected());
    }

    public void doEvolveXMinCheckBoxClicked() {
        if (doEvolveXMinCheckBox.isSelected()) {
            App.getInstance().getWrapper().setDoEvolveBounds(true);
        }
        App.getInstance().getWrapper().getMinX().setDoEvolve(doEvolveXMinCheckBox.isSelected());
    }

    public void xMinEvolutionCustomizeButtonClicked() {
        if (xMinEvolutionCustomizer == null) {
            xMinEvolutionCustomizer = createCustomizer("xMin", App.getInstance().getWrapper().getMinX());
        }
        xMinEvolutionCustomizer.show();
        xMinEvolutionCustomizer.toFront();
    }

    public void doEvolveXMaxCheckBoxClicked() {
        if (doEvolveXMaxCheckBox.isSelected()) {
            App.getInstance().getWrapper().setDoEvolveBounds(true);
        }
        App.getInstance().getWrapper().getMaxX().setDoEvolve(doEvolveXMaxCheckBox.isSelected());
    }

    public void xMaxEvolutionCustomizeButtonClicked() {
        if (xMaxEvolutionCustomizer == null) {
            xMaxEvolutionCustomizer = createCustomizer("xMax", App.getInstance().getWrapper().getMaxX());
        }
        xMaxEvolutionCustomizer.show();
        xMaxEvolutionCustomizer.toFront();
    }

    public void doEvolveYMinCheckBoxClicked() {
        if (doEvolveYMinCheckBox.isSelected()) {
            App.getInstance().getWrapper().setDoEvolveBounds(true);
        }
        App.getInstance().getWrapper().getMinY().setDoEvolve(doEvolveYMinCheckBox.isSelected());
    }

    public void yMinEvolutionCustomizeButtonClicked() {
        if (yMinEvolutionCustomizer == null) {
            yMinEvolutionCustomizer = createCustomizer("yMin", App.getInstance().getWrapper().getMinY());
        }
        yMinEvolutionCustomizer.show();
        yMinEvolutionCustomizer.toFront();
    }

    public void doEvolveYMaxCheckBoxClicked() {
        if (doEvolveYMaxCheckBox.isSelected()) {
            App.getInstance().getWrapper().setDoEvolveBounds(true);
        }
        App.getInstance().getWrapper().getMaxY().setDoEvolve(doEvolveYMaxCheckBox.isSelected());
    }

    public void yMaxEvolutionCustomizeButtonClicked() {
        if (yMaxEvolutionCustomizer == null) {
            yMaxEvolutionCustomizer = createCustomizer("yMax", App.getInstance().getWrapper().getMaxY());
        }
        yMaxEvolutionCustomizer.show();
        yMaxEvolutionCustomizer.toFront();
    }

    public void doRecomputeFractalCheckBoxClicked() {
        setRecomputeChecked(doRecomputeFractalCheckBox.isSelected());
        App.getInstance().getWrapper().setDoRecomputeFractal(doRecomputeFractalCheckBox.isSelected());
    }

    public void applyRunSettingsButtonClicked() {
        try {
            int iterCount = Integer.parseInt(iterCountTextField.getText());
            int runCount = Integer.parseInt(runCountTextField.getText());
            int workSize = Integer.parseInt(workItemsTextField.getText());
            int skip = Integer.parseInt(skipCountTextField.getText());

            App.getInstance().getWrapper().getNewtonKernelWrapper()
                    .setRunParams(workSize, runCount, iterCount, skip);
        } catch (NumberFormatException ignored) {
        }
        String color = colorTextBox.getText();
        float[] colorFloatComponents = new float[4];
        if (color != null && !color.isEmpty()) {
            String[] colorComponents = color.split("\\s*,\\s*");
            for (int i = 0; i < colorFloatComponents.length; ++i) {
                if (i < colorComponents.length) {
                    try {
                        colorFloatComponents[i] = Float.parseFloat(colorComponents[i]);
                    } catch (NumberFormatException ignored) {
                        colorFloatComponents[i] = 0.5f;
                    }
                } else {
                    colorFloatComponents[i] = 0.5f;
                }
            }
            if (colorFloatComponents.length - colorComponents.length == 1) {
                colorFloatComponents[colorComponents.length] = 1.0f; // set alpha
            }
        }
        App.getInstance().getWrapper().getNewtonKernelWrapper().setColor(
                colorFloatComponents[0],
                colorFloatComponents[1],
                colorFloatComponents[2],
                colorFloatComponents[3]
        );
    }

    private Stage createCustomizer(String name, EvolvableParameter parameter) {
        try {
            ParamCustomizationPane p = new ParamCustomizationPane(parameter);
            Scene customizationScene = new Scene(p);
            Stage stage = new Stage(StageStyle.UNIFIED);
            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    p.initialize();
                }
            });
            stage.setScene(customizationScene);
            stage.setTitle("Customize " + name);
            stage.setOnCloseRequest((event) -> {
                stage.hide();
                event.consume();
            });
            return stage;
        } catch (NoSuchResourceException e) {
            throw new RuntimeException("cannot create customizer", e);
        }
    }

    private double scaleAndClamp(double scale, EvolvableParameter parameter) {
        return max(
                min(parameter.getValue() * scale, parameter.getUpper()),
                parameter.getLower()
        );
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            watcherRunning = false;
            watcher.join(pause);
        } catch (Throwable ignored) {
        }
    }

    public void saveImageButtonClicked() {
        App.getInstance().getWrapper().setSaveScreenshot(true);
        // saveImageButton.setDisable(true);
    }

    public void timeDirectionToggled(MouseEvent actionEvent) {
        boolean newValue = App.getInstance().getWrapper().toggleDirection();
        if (!newValue) {
            timeDirectionToggle.setText("n -> n+1");
        } else {
            timeDirectionToggle.setText("n+1 -> n");
        }
    }

    public void tToggle(MouseEvent mouseEvent) {
        int newValue = App.getInstance().getWrapper().toggleT();
        if (newValue > 0) {
            tToggleButton.setText("t = 1");
        } else {
            tToggleButton.setText("t = -1");
        }
    }
}
