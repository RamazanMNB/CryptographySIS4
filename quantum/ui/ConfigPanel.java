package quantum.quantum.ui;

import quantum.quantum.SimulationConfig;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Left-side configuration panel with all user-adjustable settings.
 */
public class ConfigPanel extends JPanel {

    private static final Color BG      = new Color(20, 24, 36);
    private static final Color BORDER  = new Color(50, 60, 80);
    private static final Color TEXT    = new Color(200, 210, 230);
    private static final Color ACCENT  = new Color(86, 182, 254);
    private static final Color EVE_COL = new Color(255, 100, 100);

    // ── Controls ──────────────────────────────────────────────────────────────
    private final JSpinner   photonSpinner;
    private final JSlider    errorCheckSlider;
    private final JSlider    errorThreshSlider;
    private final JToggleButton eveToggle;
    private final JSlider    noiseSlider;
    private final JCheckBox  cascadeCheck;

    // ── Action buttons ────────────────────────────────────────────────────────
    private final JButton runButton;
    private final JButton stepButton;
    private final JButton resetButton;

    // Labels that update with slider value
    private final JLabel errorCheckLabel  = new JLabel("10%");
    private final JLabel errorThreshLabel = new JLabel("11%");
    private final JLabel noiseLabel       = new JLabel("0%");

    private Consumer<SimulationConfig> onRun;
    private Consumer<SimulationConfig> onStep;
    private Runnable onReset;

    // =========================================================================
    public ConfigPanel() {
        setBackground(BG);
        setPreferredSize(new Dimension(220, 600));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Title
        JLabel title = styled(new JLabel("Configuration"), Font.BOLD, 14, ACCENT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);
        add(vgap(8));
        add(separator());
        add(vgap(10));

        // ── Photon count ──────────────────────────────────────────────────────
        add(label("Number of photons"));
        photonSpinner = new JSpinner(new SpinnerNumberModel(500, 10, 10000, 100));
        style(photonSpinner);
        add(photonSpinner);
        add(vgap(10));

        // ── Error check % ─────────────────────────────────────────────────────
        add(rowWithValue("Error check %", errorCheckLabel));
        errorCheckSlider = makeSlider(5, 25, 10);
        errorCheckSlider.addChangeListener(e ->
                errorCheckLabel.setText(errorCheckSlider.getValue() + "%"));
        add(errorCheckSlider);
        add(vgap(10));

        // ── Error threshold ───────────────────────────────────────────────────
        add(rowWithValue("Error threshold", errorThreshLabel));
        errorThreshSlider = makeSlider(5, 30, 11);
        errorThreshSlider.addChangeListener(e ->
                errorThreshLabel.setText(errorThreshSlider.getValue() + "%"));
        add(errorThreshSlider);
        add(vgap(10));

        // ── Channel noise ─────────────────────────────────────────────────────
        add(rowWithValue("Channel noise", noiseLabel));
        noiseSlider = makeSlider(0, 5, 0);
        noiseSlider.addChangeListener(e ->
                noiseLabel.setText(noiseSlider.getValue() + "%"));
        add(noiseSlider);
        add(vgap(14));
        add(separator());
        add(vgap(10));

        // ── Eve toggle ────────────────────────────────────────────────────────
        eveToggle = new JToggleButton("Eve: OFF");
        eveToggle.setAlignmentX(LEFT_ALIGNMENT);
        eveToggle.setMaximumSize(new Dimension(200, 32));
        eveToggle.setBackground(new Color(40, 24, 24));
        eveToggle.setForeground(new Color(180, 100, 100));
        eveToggle.setFont(new Font("SansSerif", Font.BOLD, 12));
        eveToggle.setFocusPainted(false);
        eveToggle.setBorder(BorderFactory.createLineBorder(new Color(80, 40, 40), 1));
        eveToggle.addActionListener(e -> {
            boolean on = eveToggle.isSelected();
            eveToggle.setText("Eve: " + (on ? "ON" : "OFF"));
            eveToggle.setBackground(on ? new Color(50, 20, 20) : new Color(40, 24, 24));
            eveToggle.setForeground(on ? EVE_COL : new Color(180, 100, 100));
        });
        add(eveToggle);
        add(vgap(10));

        // ── Cascade error correction ──────────────────────────────────────────
        cascadeCheck = new JCheckBox("Cascade error correction  (+3pts)");
        cascadeCheck.setBackground(BG);
        cascadeCheck.setForeground(new Color(170, 140, 230));
        cascadeCheck.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cascadeCheck.setAlignmentX(LEFT_ALIGNMENT);
        add(cascadeCheck);
        add(vgap(14));
        add(separator());
        add(vgap(12));

        // ── Buttons ───────────────────────────────────────────────────────────
        runButton   = makeButton("▶  Run",       new Color(40, 100, 60),  new Color(80, 200, 120));
        stepButton  = makeButton("⏭  Step mode", new Color(30, 60, 100),  ACCENT);
        resetButton = makeButton("↺  Reset",     new Color(50, 40, 20),   new Color(255, 180, 50));

        runButton.addActionListener(e   -> { if (onRun   != null) onRun.accept(getConfig()); });
        stepButton.addActionListener(e  -> { if (onStep  != null) onStep.accept(getConfig()); });
        resetButton.addActionListener(e -> { if (onReset != null) onReset.run(); });

        add(runButton);
        add(vgap(6));
        add(stepButton);
        add(vgap(6));
        add(resetButton);
        add(Box.createVerticalGlue());
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    public SimulationConfig getConfig() {
        SimulationConfig cfg = new SimulationConfig();
        cfg.numPhotons         = (int) photonSpinner.getValue();
        cfg.errorCheckFraction = errorCheckSlider.getValue() / 100.0;
        cfg.errorThreshold     = errorThreshSlider.getValue() / 100.0;
        cfg.eveEnabled         = eveToggle.isSelected();
        cfg.channelNoise       = noiseSlider.getValue() / 100.0;
        cfg.useCascade         = cascadeCheck.isSelected();
        return cfg;
    }

    public void setOnRun(Consumer<SimulationConfig> cb)    { this.onRun   = cb; }
    public void setOnStep(Consumer<SimulationConfig> cb)   { this.onStep  = cb; }
    public void setOnReset(Runnable cb)                    { this.onReset = cb; }
    public void setRunning(boolean running) {
        runButton.setEnabled(!running);
        stepButton.setEnabled(!running);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(130, 145, 175));
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel rowWithValue(String text, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(200, 18));
        row.add(label(text), BorderLayout.WEST);
        valueLabel.setForeground(ACCENT);
        valueLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JSlider makeSlider(int min, int max, int val) {
        JSlider s = new JSlider(min, max, val);
        s.setBackground(BG);
        s.setForeground(TEXT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        s.setMaximumSize(new Dimension(200, 30));
        return s;
    }

    private void style(JSpinner sp) {
        sp.setBackground(new Color(30, 36, 54));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(200, 28));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBackground(new Color(30, 36, 54));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(TEXT);
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(200, 34));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg.darker(), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        return b;
    }

    private JLabel styled(JLabel l, int style, int size, Color col) {
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(col);
        return l;
    }

    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);
        sep.setMaximumSize(new Dimension(200, 1));
        return sep;
    }
}
