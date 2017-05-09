package com.github.modelflat.coursework2.ui;

import com.github.modelflat.coursework2.App;
import com.github.modelflat.coursework2.core.EvolvableParameter;
import com.github.modelflat.coursework2.core.MyGLCanvasWrapper;
import com.github.modelflat.coursework2.util.NoSuchResourceException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Created on 06.05.2017.
 */
public class MainControlPaneController {

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
    private CheckBox doEvolveTCheckBox;
    @FXML
    private Button tEvolutionCustomizeButton;
    private Stage tEvolutionCustomizer;
    @FXML
    private Label tCurrentStateLabel;
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
        doEvolveTCheckBox.setSelected(wrapper.getT().evolving());
        doEvolveCRealCheckBox.setSelected(wrapper.getcReal().evolving());
        doEvolveCImagCheckBox.setSelected(wrapper.getcImag().evolving());
        setEvolveBoundsChecked(wrapper.doEvolveBounds());
        doEvolveXMinCheckBox.setSelected(wrapper.getMinX().evolving());
        doEvolveXMaxCheckBox.setSelected(wrapper.getMaxX().evolving());
        doEvolveYMinCheckBox.setSelected(wrapper.getMinY().evolving());
        doEvolveYMaxCheckBox.setSelected(wrapper.getMaxY().evolving());

        // set up parameter watcher thread
        watcher = new Thread(() -> {
            while (watcherRunning) {
                MyGLCanvasWrapper wrapper1 = App.getInstance().getWrapper();

                String tText = String.format("t = %.12g", wrapper1.getT().getValue());
                String cRealText = String.format("cReal = %.12g", wrapper1.getcReal().getValue());
                String cImagText = String.format("cImag = %.12g", wrapper1.getcImag().getValue());
                String minXText = String.format("minX = %.12g", wrapper1.getMinX().getValue());
                String maxXText = String.format("maxX = %.12g", wrapper1.getMaxX().getValue());
                String minYText = String.format("minY = %.12g", wrapper1.getMinY().getValue());
                String maxYText = String.format("maxY = %.12g", wrapper1.getMaxY().getValue());

                Platform.runLater(() -> {
                    tCurrentStateLabel.setText(tText);
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

    public void doEvolveTCheckBoxClicked() {
        App.getInstance().getWrapper().getT().setDoEvolve(doEvolveTCheckBox.isSelected());
    }

    public void tEvolutionCustomizeButtonClicked() {
        if (tEvolutionCustomizer == null) {
            tEvolutionCustomizer = createCustomizer("t", App.getInstance().getWrapper().getT());
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            watcherRunning = false;
            watcher.join(pause);
        } catch (Throwable ignored) {
        }
    }
}
