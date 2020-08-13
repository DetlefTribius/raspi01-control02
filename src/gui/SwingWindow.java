package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.math.BigDecimal;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Das SwingWindow ist ein JFrame (GUI-Swing-Klasse).
 * Es gestaltet die GUI mit einer Liste und den 
 * entsprechenden Buttons.
 * </p>
 * <p>
 * Die Datenhaltung erfolgt im Model.
 * </p> 
 * <p>
 * Vgl.: https://dbs.cs.uni-duesseldorf.de/lehre/docs/java/javabuch/html/k100242.html<br>
 * Auch: http://www.willemer.de/informatik/java/guimodel.htm<br>
 * </p>
 * <p>
 * Radio-Button: http://www.fredosaurus.com/notes-java/GUI/components/50radio_buttons/25radiobuttons.html
 * </p>
 * @author Detlef Tribius
 *
 */
public class SwingWindow extends JFrame implements View   
{
    /**
     * serialVersionUID = 1L - durch Eclipse generiert...
     */
    private static final long serialVersionUID = 1L;

    /**
     * logger - Instanz zur Protokollierung...
     */
    private final static Logger logger = LoggerFactory.getLogger(SwingWindow.class);      

    /**
     * textComponentMap - nimmt die Controls zur Darstellung der Daten (hier JTextField) auf...
     */
    private final java.util.Map<String, JTextComponent> textComponentMap = new java.util.TreeMap<>();
    
    private final java.util.Map<String, JComboBox> comboBoxMap = new java.util.TreeMap<>();
  
    private static final String TEXT_FIELD = JTextField.class.getCanonicalName();
    
    private static final String COMBO_BOX = JComboBox.class.getCanonicalName();
    
    /**
     * controlData - Beschreibungsdaten der Oberflaechenelemente...
     */
    private final static String[][] controlData = new String[][]
    {
        {TEXT_FIELD, Data.COUNTER_KEY,          "Lfd. Nr." },
        {TEXT_FIELD, Data.PHI_KEY,              "Impulse" },
        {TEXT_FIELD, Data.ROTATION_KEY,         "Umdrehungen" },
        {TEXT_FIELD, Data.LAP_TIME_KEY,         "Drehzeit [s]" },
        {TEXT_FIELD, Data.RPM_KEY,              "Drehzahl [1/min]" },
        {TEXT_FIELD, Data.CYCLE_TIME_KEY,       "Taktzeit [s]" },
        {COMBO_BOX,  Model.DATA_SET_POINT_KEY,  "Sollwert" }
    };
    
    
    /**
     * 
     */
    private ActionListener actionListener = null; 
    
    /**
     * Reset-Button...
     */
    private final JButton resetButton = new JButton("Reset");
    
    /**
     * Start-Button...
     */
    private final JButton startButton = new JButton("Start");
    
    /**
     * Stop-Button...
     */
    private final JButton stopButton = new JButton("Stop");
    
    /**
     * Ende-Button... beendet die Anwendung
     */
    private final JButton endButton = new JButton("Ende");
    
    /**
     * 
     */
    private final JButton buttons[] = new JButton[] 
    { 
        resetButton,
        startButton,
        stopButton,
        endButton
    };
    
    /**
     * jContentPane - Referenz auf das Haupt-JPanel 
     */
    private JPanel jContentPane = null;
    
    /**
     * This is the default constructor
     */
    public SwingWindow(Model model)
    {
        super();
        initialize();
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                logger.debug("windowClosing(WindowEvent)...");
                model.shutdown();
                System.exit(0);
            }
        });
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize()
    {
        this.setSize(450, 250);
        this.setContentPane(getJContentPane());
        this.setTitle( "GPIO-Motorsteuerung (DRV8830)" );
        this.resetButton.setName(Model.NAME_RESET_BUTTON);
        this.startButton.setName(Model.NAME_START_BUTTON);
        this.stopButton.setName(Model.NAME_STOP_BUTTON);
        this.endButton.setName(Model.NAME_END_BUTTON);
    }

    /**
     * This method initializes jContentPane
     * 
     * getJContentPane() - Methode baut das SwingWindow-Fenster auf.
     * Es werden alle sichtbaren Komponenten instanziiert.
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane()
    {
        if (jContentPane == null)
        {
            jContentPane = new JPanel();
            // BorderLayout hat die Bereiche
            // BorderLayou.NORTH
            // BorderLayout.CENTER
            // BorderLayout.SOUTH
            jContentPane.setLayout(new BorderLayout(10, 10));
            
            {   // NORTH
                JPanel northPanel = new JPanel();
                northPanel.setLayout(new BoxLayout(northPanel, javax.swing.BoxLayout.Y_AXIS));
                
                // northPanel wird in den Bereich NORTH eingefuegt.
                jContentPane.add(northPanel, BorderLayout.NORTH);
            }
            
            { // WEST
                // leeres Panel (Platzhalter)...
                jContentPane.add(new JPanel(), BorderLayout.WEST);
            }
            
            { // EAST
                // leeres Panel (Platzhalter)...
                jContentPane.add(new JPanel(), BorderLayout.EAST);
            }
            
            {   // CENTER
                // Struktur: centerPanel als BoxLayout, Ausrichtung von oben nach unten.
                // Jede Zelle erneut als BoxLayout von links nach rechts.
                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS));
                
                for(String[] controlParam: SwingWindow.controlData)
                {
                    final String controlType = controlParam[0];
                    final String controlId = controlParam[1];
                    final String labelText = controlParam[2];
                    {
                        JPanel controlPanel = new JPanel();
                        controlPanel.setLayout(new BoxLayout(controlPanel, javax.swing.BoxLayout.X_AXIS));
                        controlPanel.add(Box.createHorizontalGlue());
                        controlPanel.add(new JLabel(labelText));
                        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
                    
                        if (TEXT_FIELD.equals(controlType))
                        {
                            JTextField controlTextField = new JTextField(10);
                            controlTextField.setMaximumSize(new Dimension(100, controlTextField.getMinimumSize().height));
                            this.textComponentMap.put(controlId, controlTextField);
                            controlTextField.setEditable(false);
                            controlPanel.add(controlTextField);
                            controlPanel.add(Box.createRigidArea(new Dimension(4, 0)));
                            centerPanel.add(controlPanel);
                        }
                        else if (COMBO_BOX.equals(controlType) && Model.DATA_SET_POINT_KEY.equals(controlId))
                        {
                            // setPointComboBox - ComboBox mit Auswahl entsprechend der Stufung Sollwertvorgabe...
                            JComboBox<BigDecimal> setPointComboBox = new JComboBox<>(Model.SET_POINTS);
                            setPointComboBox.setName(controlId);
                            setPointComboBox.setMaximumSize(new Dimension(100, setPointComboBox.getMinimumSize().height));
                            this.comboBoxMap.put(controlId, setPointComboBox); 
                            controlPanel.add(setPointComboBox);
                            controlPanel.add(Box.createRigidArea(new Dimension(4, 0)));
                            centerPanel.add(controlPanel);
                            
                            // Selektion des Eintrages mit BigDecimal.ZERO...
                            setPointComboBox.setSelectedIndex(Model.SELECTED_INDEX);
                            
                            setPointComboBox.addActionListener(new ActionListener() 
                            {

                                @Override
                                public void actionPerformed(ActionEvent event)
                                {
                                    JComboBox<BigDecimal> source = (JComboBox<BigDecimal>)event.getSource();   
                                    logger.info(source.getName() + ": " + event.getActionCommand());   
                                    
                                    actionCommandDelegate(event);
                                }
                            });
                        }
                    }

                    {
                        // Leerzeile...
                        JPanel emptyPanel = new JPanel();
                        emptyPanel.setLayout(new BoxLayout(emptyPanel, javax.swing.BoxLayout.Y_AXIS));
                        emptyPanel.add(Box.createRigidArea(new Dimension(0, 4)));
                        centerPanel.add(emptyPanel);
                    }
                }
                
                jContentPane.add(centerPanel, BorderLayout.CENTER);
            }
            
            {   // SOUTH...
                // buttonPanel beinhaltet die Button...
                JPanel buttonPanel = new JPanel();
                FlowLayout flowLayout = (FlowLayout) buttonPanel.getLayout();
                flowLayout.setAlignment(FlowLayout.RIGHT);
            
                for(JButton button: buttons)
                {
                    button.setHorizontalAlignment(SwingConstants.RIGHT);
                    button.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent event)
                        {
                            final JButton source = (JButton)event.getSource();
                            logger.debug(source.getName());
                            //
                            actionCommandDelegate(event);
                        }
                    });
                    //
                    buttonPanel.add(button);
                }
                
                jContentPane.add(buttonPanel, BorderLayout.SOUTH);
            }
        }
        return jContentPane;
    }

    @Override
    public void addActionListener(ActionListener listener)
    {
        logger.debug("Controller hinzugefuegt (ActionListener)...");
        this.actionListener = listener;
    }

    /**
     * propertyChange(PropertyChangeEvent event) - wird vom Model her beaufragt
     * und muss die View evtl. nachziehen...  
     */
    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        final String propertyName = event.getPropertyName();
        final Object newValue = event.getNewValue();

        if (Model.DATA_KEY.equals(propertyName))
        {
            // propertyChange vom Model her mit DATA_KEY...
            if (newValue instanceof Data)
            {
                Data newData = (Data) newValue;
                for(String key: newData.getKeys())
                {
                    if (this.textComponentMap.containsKey(key))
                    {
                        final JTextComponent textComponent = this.textComponentMap.get(key);
                        textComponent. setText(newData.getValue(key));
                        continue;
                    }
                }
            }
        }
        if (Model.DATA_SET_POINT_KEY.equals(propertyName))
        {
            // propertyChange vom Model her mit DATA_SET_POINT_KEY...
            BigDecimal newData = (BigDecimal) newValue;
            
            if (this.comboBoxMap.containsKey(propertyName))
            {
                JComboBox<BigDecimal> setPointComboBox = this.comboBoxMap.get(propertyName);
                setPointComboBox.setSelectedItem(newData);
            }
        }
        
        // Kontrollausgabe im Debuglevel...
        // logger.debug(event.toString());
    }

    /**
     * 
     * @param event
     */
    private void actionCommandDelegate(java.awt.event.ActionEvent event) 
    {                                       
        if (this.actionListener != null) 
        {
            this.actionListener.actionPerformed(event);
        }
    }
}
