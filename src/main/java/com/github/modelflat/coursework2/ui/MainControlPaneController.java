package com.github.modelflat.coursework2.ui;

import com.github.modelflat.coursework2.App;
import com.github.modelflat.coursework2.core.MyGLCanvasWrapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

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
    @FXML
    private Label tCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveCRealCheckBox;
    @FXML
    private Button cRealEvolutionCustomizeButton;
    @FXML
    private Label cRealCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveCImagCheckBox;
    @FXML
    private Button cImagEvolutionCustomizeButton;
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
    @FXML
    private Label xMinCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveXMaxCheckBox;
    @FXML
    private Button xMaxEvolutionCustomizeButton;
    @FXML
    private Label xMaxCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveYMinCheckBox;
    @FXML
    private Button yMinEvolutionCustomizeButton;
    @FXML
    private Label yMinCurrentStateLabel;
    //
    @FXML
    private CheckBox doEvolveYMaxCheckBox;
    @FXML
    private Button yMaxEvolutionCustomizeButton;
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
        doEvolveTCheckBox.setSelected(wrapper.doEvolveOnT());
        doEvolveCRealCheckBox.setSelected(wrapper.doEvolveOnCReal());
        doEvolveCImagCheckBox.setSelected(wrapper.doEvolveOnCImag());
        setEvolveBoundsChecked(wrapper.doEvolveBounds());
        doEvolveXMinCheckBox.setSelected(wrapper.doEvolveOnMinX());
        doEvolveXMaxCheckBox.setSelected(wrapper.doEvolveOnMaxX());
        doEvolveYMinCheckBox.setSelected(wrapper.doEvolveOnMinY());
        doEvolveYMaxCheckBox.setSelected(wrapper.doEvolveOnMaxY());

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
            wrapper.setDoEvolveOnMinX(state);
            doEvolveXMaxCheckBox.setSelected(state);
            wrapper.setDoEvolveOnMaxX(state);
            doEvolveYMinCheckBox.setSelected(state);
            wrapper.setDoEvolveOnMinY(state);
            doEvolveYMaxCheckBox.setSelected(state);
            wrapper.setDoEvolveOnMaxY(state);
        }
    }

    private void setRecomputeChecked(boolean state) {
        doRecomputeFractalCheckBox.setSelected(state);
        metaEvolutionPaneVBox.setDisable(!state);
    }

    public void doPostClearCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoPostCLear(doPostClearCheckBox.isSelected());
    }

    public void doEvolveCheckBoxClicked(MouseEvent mouseEvent) {
        setEvolveChecked(doEvolveCheckBox.isSelected());
        App.getInstance().getWrapper().setDoEvolve(doEvolveCheckBox.isSelected());
    }

    public void doCLClearCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoCLClear(doCLClearCheckBox.isSelected());
    }

    public void doEvolveTCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnT(doEvolveTCheckBox.isSelected());
    }

    public void tEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveCRealCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnCReal(doEvolveCRealCheckBox.isSelected());
    }

    public void cRealEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveCImagCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnCImag(doEvolveCImagCheckBox.isSelected());
    }

    public void cImagEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveBoundsCheckBoxClicked(MouseEvent mouseEvent) {
        setEvolveBoundsChecked(doEvolveBoundsCheckBox.isSelected());
        App.getInstance().getWrapper().setDoEvolveBounds(doEvolveBoundsCheckBox.isSelected());
    }

    public void doEvolveXMinCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnMinX(doEvolveXMinCheckBox.isSelected());
    }

    public void xMinEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveXMaxCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnMaxX(doEvolveXMaxCheckBox.isSelected());
    }

    public void xMaxEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveYMinCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnMinY(doEvolveYMinCheckBox.isSelected());
    }

    public void yMinEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doEvolveYMaxCheckBoxClicked(MouseEvent mouseEvent) {
        App.getInstance().getWrapper().setDoEvolveOnMaxY(doEvolveYMaxCheckBox.isSelected());
    }

    public void yMaxEvolutionCustomizeButtonClicked(MouseEvent mouseEvent) {
        System.out.println("customization requested...");
    }

    public void doRecomputeFractalCheckBoxClicked(MouseEvent mouseEvent) {
        setRecomputeChecked(doRecomputeFractalCheckBox.isSelected());
        App.getInstance().getWrapper().setDoRecomputeFractal(doRecomputeFractalCheckBox.isSelected());
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
