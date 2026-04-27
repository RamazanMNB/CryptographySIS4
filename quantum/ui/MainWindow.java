package quantum.quantum.ui;

import quantum.quantum.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Main application window.
 *
 * Layout:
 *   LEFT:   ConfigPanel (settings + buttons)
 *   CENTER: JTabbedPane → BB84 tab, E91 tab
 *   RIGHT:  StatsChart
 */
public class MainWindow extends JFrame {

    private static final Color BG      = new Color(15, 17, 26);
    private static final Color TAB_BG  = new Color(20, 24, 36);
    private static final Color ACCENT  = new Color(86, 182, 254);
    private static final Color TEXT    = new Color(200, 210, 230);

    // ── Layout components ────────────────────────────────────────────────────
    private final ConfigPanel  configPanel = new ConfigPanel();
    private final PhotonCanvas photonCanvas;
    private final StatsChart   statsChart  = new StatsChart();
    private final JTextArea    logArea;
    private final JLabel       keyDisplay;
    private final JLabel       phaseLabel;
    private final JProgressBar phaseBar;

    // ── Step mode ────────────────────────────────────────────────────────────
    private List<StepData> stepList   = null;
    private int            stepIndex  = 0;
    private boolean        stepMode   = false;
    private SimulationConfig stepCfg  = null;

    // ── Step mode controls ────────────────────────────────────────────────────
    private final JButton nextStepBtn;
    private final JButton prevStepBtn;
    private final JLabel  stepCountLabel;

    // =========================================================================
    public MainWindow() {
        super("BB84 + E91 Quantum Key Distribution Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(BG);
        getContentPane().setBackground(BG);

        photonCanvas   = new PhotonCanvas();
        logArea        = makeLogArea();
        keyDisplay     = makeKeyLabel();
        phaseLabel     = new JLabel("Ready");
        phaseBar       = makeProgressBar();
        nextStepBtn    = makeButton("Next →", ACCENT);
        prevStepBtn    = makeButton("← Prev", new Color(100, 120, 160));
        stepCountLabel = new JLabel(" ");

        buildUI();
        wireCallbacks();

        pack();
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
    }

    // =========================================================================
    // UI BUILD
    // =========================================================================

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(6, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // LEFT
        root.add(configPanel, BorderLayout.WEST);

        // CENTER: tabs
        JTabbedPane tabs = makeTabs();
        root.add(tabs, BorderLayout.CENTER);

        // RIGHT
        JScrollPane statsScroll = new JScrollPane(statsChart,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        statsScroll.setBackground(BG);
        statsScroll.setBorder(null);
        statsScroll.setPreferredSize(new Dimension(260, 600));
        root.add(statsScroll, BorderLayout.EAST);

        setContentPane(root);
    }

    private JTabbedPane makeTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(TAB_BG);
        tabs.setForeground(TEXT);
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));

        tabs.addTab("BB84 Protocol",  makeBB84Tab());
        tabs.addTab("E91 Protocol",   makeE91Tab());

        return tabs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private JPanel makeBB84Tab() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);

        // Top: phase progress bar
        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBackground(new Color(20, 24, 36));
        topBar.setBorder(new EmptyBorder(6, 10, 6, 10));
        phaseLabel.setForeground(ACCENT);
        phaseLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        topBar.add(phaseLabel, BorderLayout.WEST);
        topBar.add(phaseBar,   BorderLayout.CENTER);
        p.add(topBar, BorderLayout.NORTH);

        // Center: photon canvas
        p.add(photonCanvas, BorderLayout.CENTER);

        // Step mode controls
        JPanel stepBar = makeStepBar();
        p.add(stepBar, BorderLayout.SOUTH);

        return p;
    }

    private JPanel makeStepBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(20, 24, 36));
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));

        prevStepBtn.setEnabled(false);
        nextStepBtn.setEnabled(false);
        stepCountLabel.setForeground(new Color(100, 120, 160));
        stepCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        bar.add(prevStepBtn);
        bar.add(nextStepBtn);
        bar.add(stepCountLabel);

        // Key display
        JPanel keyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        keyRow.setBackground(new Color(20, 24, 36));
        JLabel keyLbl = new JLabel("Final key: ");
        keyLbl.setForeground(new Color(100, 120, 160));
        keyLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        keyDisplay.setFont(new Font("Monospaced", Font.PLAIN, 11));
        keyDisplay.setForeground(new Color(80, 200, 120));
        keyRow.add(keyLbl);
        keyRow.add(keyDisplay);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(20, 24, 36));
        bottom.add(bar, BorderLayout.WEST);
        bottom.add(keyRow, BorderLayout.CENTER);

        // Log area
        JScrollPane logScroll = new JScrollPane(logArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.setPreferredSize(new Dimension(0, 90));
        logScroll.setBorder(null);
        logScroll.setBackground(new Color(12, 14, 22));

        JPanel south = new JPanel(new BorderLayout());
        south.add(bottom,    BorderLayout.NORTH);
        south.add(logScroll, BorderLayout.CENTER);
        return south;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private JPanel makeE91Tab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        // E91 canvas (simpler visualization)
        E91Canvas e91Canvas = new E91Canvas();
        p.add(e91Canvas, BorderLayout.CENTER);

        // Run E91 button
        JButton runE91 = makeButton("▶  Run E91", new Color(40, 80, 60), new Color(80, 200, 120));
        runE91.setPreferredSize(new Dimension(160, 34));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.setBackground(BG);
        btnRow.add(runE91);

        JLabel e91Info = new JLabel("<html><font color='#6478A0' size='2'>" +
                "E91 uses entangled photon pairs. Security verified via Bell's inequality (CHSH test)." +
                "</font></html>");
        btnRow.add(e91Info);
        p.add(btnRow, BorderLayout.SOUTH);

        runE91.addActionListener(e -> {
            SimulationConfig cfg = configPanel.getConfig();
            log("Running E91 protocol... " + (cfg.eveEnabled ? "[Eve enabled]" : ""));
            setPhase("Running E91...", 0);

            SwingWorker<E91Protocol.E91Result, Void> worker = new SwingWorker<>() {
                protected E91Protocol.E91Result doInBackground() {
                    return new E91Protocol(cfg.numPhotons, cfg.eveEnabled).run();
                }
                protected void done() {
                    try {
                        E91Protocol.E91Result r = get();
                        e91Canvas.showResult(r);
                        statsChart.updateE91(r);
                        log(r.toString());
                        setPhase(r.aborted ? "E91 ABORTED" : "E91 SUCCESS", 100);
                    } catch (Exception ex) {
                        log("E91 error: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });

        return p;
    }

    // =========================================================================
    // CALLBACKS
    // =========================================================================

    private void wireCallbacks() {
        // ── Full run ──────────────────────────────────────────────────────────
        configPanel.setOnRun(cfg -> {
            resetUI();
            configPanel.setRunning(true);
            log("Running BB84 with " + cfg.numPhotons + " photons..." +
                    (cfg.eveEnabled ? " [Eve enabled]" : "") +
                    (cfg.useCascade ? " [Cascade]" : ""));
            setPhase("Phase 1: Quantum Transmission", 10);

            SwingWorker<BB84Result, String> worker = new SwingWorker<>() {
                protected BB84Result doInBackground() {
                    publish("Phase 2: Basis Reconciliation");
                    BB84Protocol protocol = new BB84Protocol(cfg);
                    BB84Result   result   = protocol.run();
                    // Animate last few steps
                    List<StepData> steps = protocol.getSteps();
                    int start = Math.max(0, steps.size() - 5);
                    for (int i = start; i < steps.size(); i++) {
                        photonCanvas.showStep(steps.get(i), cfg.eveEnabled);
                        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                        publish("Phase 3: Error Checking (" + (i + 1) + "/" + steps.size() + ")");
                    }
                    return result;
                }
                protected void process(List<String> chunks) {
                    String last = chunks.get(chunks.size() - 1);
                    phaseLabel.setText(last);
                }
                protected void done() {
                    try {
                        BB84Result r = get();
                        statsChart.updateBB84(r);
                        log(r.toString());
                        String finalKey = buildKeyPreview(r);
                        keyDisplay.setText(finalKey);
                        if (r.aborted)
                             setPhase("ABORTED — eavesdropper detected (error rate: " +
                                     String.format("%.1f%%", r.errorRate * 100) + ")", 100);
                        else setPhase("SUCCESS — final key: " + r.finalKeyLength + " bits", 100);
                    } catch (Exception ex) { log("Error: " + ex.getMessage()); }
                    configPanel.setRunning(false);
                }
            };
            worker.execute();
        });

        // ── Step mode ─────────────────────────────────────────────────────────
        configPanel.setOnStep(cfg -> {
            resetUI();
            stepMode = true;
            stepCfg  = cfg;
            log("Step mode: " + cfg.numPhotons + " photons");

            SwingWorker<BB84Protocol, Void> worker = new SwingWorker<>() {
                protected BB84Protocol doInBackground() {
                    BB84Protocol p = new BB84Protocol(cfg);
                    p.run();
                    return p;
                }
                protected void done() {
                    try {
                        BB84Protocol p = get();
                        stepList  = p.getSteps();
                        stepIndex = 0;
                        nextStepBtn.setEnabled(stepList.size() > 0);
                        prevStepBtn.setEnabled(false);
                        updateStepCount();
                        log("Ready — press Next to step through " + stepList.size() + " photons");
                    } catch (Exception ex) { log("Error: " + ex.getMessage()); }
                }
            };
            worker.execute();
        });

        // ── Step controls ─────────────────────────────────────────────────────
        nextStepBtn.addActionListener(e -> {
            if (stepList != null && stepIndex < stepList.size()) {
                StepData s = stepList.get(stepIndex++);
                photonCanvas.showStep(s, stepCfg != null && stepCfg.eveEnabled);
                logStep(s);
                prevStepBtn.setEnabled(stepIndex > 1);
                nextStepBtn.setEnabled(stepIndex < stepList.size());
                updateStepCount();
            }
        });
        prevStepBtn.addActionListener(e -> {
            if (stepList != null && stepIndex > 1) {
                stepIndex -= 2;
                StepData s = stepList.get(stepIndex++);
                photonCanvas.showStep(s, stepCfg != null && stepCfg.eveEnabled);
                logStep(s);
                prevStepBtn.setEnabled(stepIndex > 1);
                nextStepBtn.setEnabled(stepIndex < stepList.size());
                updateStepCount();
            }
        });

        // ── Reset ─────────────────────────────────────────────────────────────
        configPanel.setOnReset(this::resetAll);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void resetUI() {
        photonCanvas.reset();
        logArea.setText("");
        keyDisplay.setText("—");
        setPhase("Ready", 0);
        nextStepBtn.setEnabled(false);
        prevStepBtn.setEnabled(false);
        stepCountLabel.setText(" ");
        stepMode = false;
        stepList = null;
    }

    private void resetAll() {
        resetUI();
        statsChart.reset();
    }

    private void setPhase(String text, int progress) {
        SwingUtilities.invokeLater(() -> {
            phaseLabel.setText(text);
            phaseBar.setValue(progress);
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void logStep(StepData s) {
        log(String.format("Photon %d | Alice: bit=%d basis=%s pol=%s | Bob: basis=%s | %s | %s",
                s.index, s.aliceBit, s.aliceBasis, StepData.polSymbol(s.alicePol),
                s.bobBasis,
                s.basesMatch ? "MATCH" : "mismatch",
                s.inSiftedKey ? (s.usedForCheck ? "used for check" : "key bit") : "discarded"));
    }

    private void updateStepCount() {
        if (stepList != null)
            stepCountLabel.setText("Step " + stepIndex + " / " + stepList.size());
    }

    private String buildKeyPreview(BB84Result r) {
        if (r.aborted || r.aliceFinalKey == null || r.aliceFinalKey.isEmpty()) return "N/A";
        int n = Math.min(64, r.aliceFinalKey.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(r.aliceFinalKey.get(i));
        if (r.aliceFinalKey.size() > 64) sb.append("…");
        return sb.toString();
    }

    private JTextArea makeLogArea() {
        JTextArea a = new JTextArea();
        a.setBackground(new Color(12, 14, 22));
        a.setForeground(new Color(140, 160, 200));
        a.setFont(new Font("Monospaced", Font.PLAIN, 11));
        a.setEditable(false);
        a.setLineWrap(true);
        a.setBorder(new EmptyBorder(4, 6, 4, 6));
        return a;
    }

    private JLabel makeKeyLabel() {
        JLabel l = new JLabel("—");
        l.setForeground(new Color(80, 200, 120));
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return l;
    }

    private JProgressBar makeProgressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setStringPainted(false);
        pb.setBackground(new Color(30, 36, 54));
        pb.setForeground(ACCENT);
        pb.setBorder(null);
        pb.setPreferredSize(new Dimension(200, 8));
        return pb;
    }

    private JButton makeButton(String text, Color fg) {
        return makeButton(text, new Color(30, 40, 60), fg);
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg.darker(), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return b;
    }
}
