package gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import raspi.hardware.i2c.DRV8830;

// Vgl. https://www.baeldung.com/java-observer-pattern
// auch https://wiki.swechsler.de/doku.php?id=java:allgemein:mvc-beispiel
// http://www.nullpointer.at/2011/02/06/howto-gui-mit-swing-teil-4-interaktion-mit-der-gui/
// http://www.javaquizplayer.com/blogposts/java-propertychangelistener-as-observer-19.html
// TableModel...
// Vgl.: https://examples.javacodegeeks.com/core-java/java-swing-mvc-example/
/**
 * 
 * Das Model haelt die Zustandsgroessen..
 *
 * 
 * 
 */
public class Model 
{
    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(Model.class);

    /**
     * Kennung isRaspi kennzeichnet, der Lauf erfolgt auf dem RasberryPi.
     * Die Kennung wird zur Laufzeit aus den Systemvariablen fuer das
     * Betriebssystem und die Architektur ermittelt. Mit dieser Kennung kann
     * die Beauftragung von Raspi-internen Programmen gesteuert werden.
     */
    private final boolean isRaspi;
    /**
     * OS_NAME_RASPI = "linux" - Kennung fuer Linux.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_NAME_RASPI = "linux";
    /**
     * OS_ARCH_RASPI = "arm" - Kennung fuer die ARM-Architektur.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_ARCH_RASPI = "arm";
    
    /**
     * Referenz auf den GPIO-controller...
     * <p>
     * Der GPIO-Controller bedient die GPIO-Schnittstelle des Raspi.
     * </p>
     * <p>
     * Der GPIO-Controller wird im Konstruktor instanziiert...
     * </p>
     */
    private final GpioController gpioController;

    
    /**
     * ADDRESS - Bus-Adresse des I2C-Bausteins, festgelegt durch
     * Verdrahtung auf dem Baustein... 
     */
    public final static int ADDRESS = 0x60; 

    /**
     * drv8830 - Referenz auf den DRV8830-Baustein unter der Adresse ADDRESS
     */
    private DRV8830 drv8830 = null;
    
    /**
     * drvSetPoint - Sollwert fuer den DRV8830 (int)
     */
    private int drvSetPoint = 0;

    /**
     * Pull-Up/Pull-Down-Einstellung...
     * <p>
     * Hier Voreinstellung auf PinPullResistance.OFF, da Pull-Down-Widerstaende 
     * durch die Hardware bereitgestellt werden...
     * </p>
     * <p>
     * Hier Einstellung: Kein Pull-Down/Pull-Up durch den Raspi (daher PinPullResistance.OFF)...
     * </p>
     */
    private final static PinPullResistance PIN_PULL_RESISTANCE = PinPullResistance.OFF;
    
    /**
     * ...der folgenden Pin wird über den Takt des Ne555 angesprochen und gibt damit den Takt
     * fuer die Regelung und Anzeige vor...
     * <p>
     * Der Name des Pins wird als Key fuer die Ablage der damit im Zusammenhang
     * stehenden Daten verwendet (Key: GPIO_PIN_NE555_NAME)
     * </p>
     * <p>
     * Der am Pin auftretende Impuls wird durch das Interface GpioPinListenerDigital 
     * verarbeitet.
     * </p>
     */
    private final static Pin GPIO_NE555_PIN = RaspiPin.GPIO_00;    // GPIO 17, Board-Nr. = 11
    
    /**
     * Impulsfolge A..., wird durch einen entsprechenden Interrupt bedient.
     * <p>
     * Die Verarbeitung der Impulsfolge erfolgt durch das Interface
     * GpioPinListenerDigital.
     * </p>
     */
    private final static Pin GPIO_INC_A_PIN = RaspiPin.GPIO_02;     // GPIO 27, Board-Nr. = 13
    
    /**
     * Impulsfolge B..., wird aber mit dem Impuls A in der Interrupt-Routine abgefragt...
     * <p>
     * Die Verarbeitung der Impulsfolge GPIO_INC_B_PIN erfolgt durch Abfrage des 
     * Zustandes ueber GpioPinDigitalInput.
     * </p>
     */
    private final static Pin GPIO_INC_B_PIN = RaspiPin.GPIO_03;     // GPIO 22, Board-Nr. = 15
    
    /**
     * gpio_Inc_B_Pin - Referenz auf den Zustand des Pin GPIO_INC_B_PIN
     */
    private final GpioPinDigitalInput gpio_Inc_B_Pin; 
    
    /**
     * GPIO_NE555_PIN_NAME - String-Name des Takt-Pins an dem der Ne555
     * angeschlossen ist.
     */
    public final static String GPIO_NE555_PIN_NAME = GPIO_NE555_PIN.getName();
    
    /**
     * GPIO_INC_A_PIN_NAME - String-Name des Pin A...
     */
    public final static String GPIO_INC_A_PIN_NAME = GPIO_INC_A_PIN.getName();
    
    /**
     * GPIO_INC_B_PIN_NAME - String-Name des Pin B...
     */
    public final static String GPIO_INC_B_PIN_NAME = GPIO_INC_B_PIN.getName();
    
    /**
     * NAME_RESET_BUTTON = "resetButton"
     */
    public static final String NAME_RESET_BUTTON = "resetButton";
    
    /**
     * NAME_START_BUTTON = "startButton"
     */
    public static final String NAME_START_BUTTON = "startButton";

    /**
     * NAME_STOP_BUTTON = "stopButton"
     */
    public static final String NAME_STOP_BUTTON = "stopButton";
    
    /**
     * NAME_END_BUTTON = "endButton"
     */
    public static final String NAME_END_BUTTON = "endButton";
    
    /**
     * dataMap - nimmt die Eingaben der GUI auf...
     * <p>
     * Ablage key => Eingabe-Object
     * </p>
     */
    private final java.util.TreeMap<String, Object>  dataMap = new java.util.TreeMap<>();
    /**
     * DATA_KEY = "dataKey" - Key zum zugriff auf die Data-Daten. Diese Daten beinhalten
     * zumeist die Informationen vom Model hin zur GUI, die in der GUI nur angezeigt werden.
     */
    public final static String DATA_KEY = "dataKey"; 
    
    /**
     * DATA_SET_POINT_KEY = "dataSetPointKey" - Key zum Zugriff auf den Sollwert.
     * Der Sollwert wird an der GUI eingegeben und zum Modell hin uebertragen.
     * Der uebertragene Wert wird dann in der GUI angezeigt. 
     */
    public final static String DATA_SET_POINT_KEY = "dataSetPointKey";
    
    /**
     * DATA_KEYS[] - Array mit den Keys zur Ablage in der dataMap...
     * <p>
     * Die Keys muessen in der Map eingetragen sein, sonst werden Datenaenderungen
     * ignoriert.
     * </p>
     */
    private final static String[] DATA_KEYS = 
    {
        DATA_KEY,
        DATA_SET_POINT_KEY
    };

    /**
     * MAX_VALUE - max. Sollwert des PWM-Schaltkreises (hier nur der Betrag!)...
     * <p>
     * Auswaehlbar sind (2*MAX_VALUE+1)-Werte von (-MAX_VALUE... 0 ...+MAX_VALUE)
     * </p>
     */
    private final static int MAX_VALUE = 31;     
    
    /**
     * SET_POINT_SCALE = 5
     */
    public static int SET_POINT_SCALE = 5;
    
    /**
     * SET_POINTS - Array mit den Sollwerten als Anzeigewerte
     * <p>
     * Der Index verweist auf den Anzeigewert, z.B <br>
     * <code>
     * -63 => -1,00<br>
     *  0 => 0,000<br>
     * 63 => 1,000<br>
     * </code>
     * </p>
     * <p>
     * Der konkrete Anzeigewert ergibt sich aus der gewaehlten NORMALIZATION.
     * </p>
     */
    public final static BigDecimal[] SET_POINTS = new BigDecimal[2*MAX_VALUE+1];
    
    /**
     * NORMALIZATION - Narmalisierung des Ausgabewertes (Sollwert 0...63)
     */
    // Normierung, Anzeiche dann -1 ... 0 ... +1:
    // public final static BigDecimal NORMALIZATION = BigDecimal.valueOf(MAX_VALUE);
    // Keine Normierung, Anzeigewert dann von -MAX_VALUE...0...+MAX_VALUE:
    public final static BigDecimal NORMALIZATION = BigDecimal.ONE;
    
    static
    {
        // Index A: (-MAX_VALUE) ...   (-1)   ...    (0)      ...    (+1) ... (+MAX_VALUE)
        // INDEX B: (    0    ) ... (MAX_VALUE-1) (MAX_VALUE)  ...        ... (2*MAX_VALUE)
        // => (Index B) - (Index  A) = MAX_VALUE
        //
        // Der Nullpunkt liegt genau bei [MAX_VALUE]...
        SET_POINTS[MAX_VALUE] = BigDecimal.ZERO.setScale(SET_POINT_SCALE, RoundingMode.FLOOR).divide(Model.NORMALIZATION, RoundingMode.FLOOR).setScale(SET_POINT_SCALE, RoundingMode.FLOOR);
        // ...davon ausgehend wird das Array gefuellt...
        for (int index = 1; index <= MAX_VALUE; index++)
        {
            final BigDecimal value = BigDecimal.valueOf(index).setScale(SET_POINT_SCALE, RoundingMode.FLOOR).divide(Model.NORMALIZATION, RoundingMode.FLOOR).setScale(SET_POINT_SCALE, RoundingMode.FLOOR);
            SET_POINTS[MAX_VALUE-index] = value;
            SET_POINTS[MAX_VALUE+index] = value.negate();
        }
    }
    
    /**
     * 
     */
    public final static int SELECTED_INDEX = MAX_VALUE;
    
    /**
     * setPointsMap - beinhaltet die Zuordnung des Anzeigewertes aus dem Array 
     * SET_POINTS zum Eingangswert des PWM-Schaltkreises. Die Map wird aus
     * dem Array SET_POINTS aufgebaut...
     * <p>
     * setPointsMap kann verwendet werden, um aus dem Anzeigewert der ComboBox
     * den Sollwert fuer den PWM-Schaltkreis zu ermitteln...
     * </p>
     */
    private final java.util.Map<BigDecimal, Integer> setPointsMap = new java.util.TreeMap<>();
    
    /**
     * support - Referenz auf den PropertyChangeSupport...
     */
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    /**
     * counter - Taktzaehler (keine weitere funktionale Bedeutung)
     */
    private long counter = 0L;
    
    /**
     * phi - Lageinformation in Impulse, Mass fuer den Winkel phi,
     * gemessen in Anzahl der Impule...
     */
    private long phi = 0L;

    /**
     *  position - das long-Array position[] dient der Ermittlung 
     *  des Zuwachses der Position waehrend der letzten Taktung: 
     *  phi[k+1]-phi[k].
     */
    private long[] position = new long[] {0L, 0L};
    
    /**
     * deltas[] - Folge der deltas, jeweils Dokumentation des Zuwachs am Lagewert...
     */
    private long[] deltas = new long[10];
    
    /**
     * is_B_High[] - Kennung fuer is_B_High der aktuellen und der
     * letzten Umdrehung, damit Möglichkeit, 
     * einen Richtungswechsel zu erkennen...
     */
    private boolean is_B_High[] = {false, false};
    
    /**
     * 
     */
    private Instant lapStartTime = null;
    
    /**
     * 
     */
    private Instant lapEndTime = null;
    
    /**
     * 
     */
    private BigDecimal lapTime = BigDecimal.ZERO;
    
    /**
     * Instant past - letzter Zeitstempel...
     * <p>
     * Der Takt wird durch den Ne555 vorgegeben. 
     * Hier wird der letzte Zeitstempel abgelegt zur Bestimmung
     * der Taktdauer T. Die Taktdauer wird in cycleTime abgelegt.
     * </p>
     * <p>
     * Der Anfangswert muss null sein, um die Erstbeauftragung zu erkennen,
     * da erst bei Zweitbeauftragung die Taktdauer bestimmbar ist.
     * </p>
     */
    private Instant past = null;
    
    /**
     * cycleTime - aktuell ermittelte Taktzeit aus (now - past)...
     */
    private Duration cycleTime = Duration.ZERO;

    /**
     * 
     */
    public final static int SCALE_INTERN = 6;
    
    /**
     * 
     */
    public final static int SCALE_RPM = 3;
    
    
    /**
     * Darstellung der Taktzeit...
     */
    public static int SCALE_CYCLE_TIME = 3;
    
    
    public static int SCALE_LAP_TIME = 3;
    
    /**
     * PULS_NUMBER = 400L - Anzahl der Impulse pro Umdrehung
     */
    public final static long PULS_NUMBER = 400L;
   
    /**
     * MEASURING_NUMBER - Anzahl der Impulse zur Ermittlung der 
     * Umdrehungsdauer.
     * <p>
     * Achtung!! Ganzzahlig-Vielfaches von MEASURING_NUMBER muss PULS_NUMBER ergeben! 
     * </p>
     */
    public final static long MEASURING_NUMBER = 100L;
    
    /**
     * MEASURING_FACTOR - Korrekturfaktor, da MEASURING_NUMBER Teiler von PULS_NUMBER. 
     */
    public final static BigDecimal MEASURING_FACTOR = BigDecimal.valueOf(PULS_NUMBER/MEASURING_NUMBER);
    
    /**
     * CIRCUMFERENCE - Anzahl der Impulse pro Umdrehung
     * 
     * Aus der Anzahl der Impulse I pro Zeiteinheit T ergibt sich die
     * Umdrehungszahl U pro Minute zu:
     * 
     *   U = I * 1/CIRCUMFERENCE * 60/T
     *   U = (I/T) * (60/CIRCUMFERENCE) 
     */
    public final static BigDecimal CIRCUMFERENCE = BigDecimal.valueOf(PULS_NUMBER);
    
    /**
     * CONST
     */
    public final static BigDecimal CONST = BigDecimal.valueOf(60L).divide(CIRCUMFERENCE, SCALE_INTERN, BigDecimal.ROUND_HALF_UP);

    
    /**
     * 
     */
    private BigDecimal cycleTimeDecimal = BigDecimal.ZERO;    
    
    /**
     * 
     */
    private BigDecimal rotation = null;
    
    /**
     * 
     */
    private BigDecimal rpm = null;
    
    /**
     * lock - Object fuer das Synchronisieren...
     */
    final private Object lock = new Object(); 
    
    /**
     * Default-Konstruktor 
     */
    public Model() 
    {
        // 1.) Wo erfolgt der Lauf, auf einem Raspi?
        final String os_name = System.getProperty("os.name").toLowerCase();
        final String os_arch = System.getProperty("os.arch").toLowerCase();
        logger.debug("Betriebssytem: " + os_name + " " + os_arch);
        // Kennung isRaspi setzen...
        this.isRaspi = OS_NAME_RASPI.equals(os_name) && OS_ARCH_RASPI.equals(os_arch);
        
        // ... Map setPointsMap befuellen...
        for (int index = 0; index < SET_POINTS.length; index++)
        {
            // index = 0          =>  -MAX_VALUE
            // index = MAX_VALUE  =>   0
            // index = 2*MAX_VALUE => +MAX_VALUE
            setPointsMap.put(SET_POINTS[index], Integer.valueOf(MAX_VALUE-index));
        }
        
        // *** Befuellen der dataMap... ***
        // Die dataMap muss mit allen Key-Eintraegen befuellt werden, sonst 
        // ist setProperty(String key, Object newValue) unwirksam!
        for (String key: Model.DATA_KEYS)
        {
            this.dataMap.put(key, null);
        }
        
        // Protokollarray zuruecksetzen...
        for (int index = 0; index < this.deltas.length; index++)
        {
            this.deltas[index] = 0L;
        }
        
        // ...den gpioController anlegen...
        this.gpioController = isRaspi? GpioFactory.getInstance() : null;

        this.gpio_Inc_B_Pin = (this.gpioController != null)? this.gpioController.provisionDigitalInputPin(GPIO_INC_B_PIN, PIN_PULL_RESISTANCE) : null;

        // Anfangswerte setzen...
        this.lapEndTime = this.lapStartTime = Instant.now();
        
        ///////////////////////////////////////////////////////////////////////////////////////////
        // Alles weitere nur, wenn der Lauf auf dem Raspi erfolgt...
        if (this.isRaspi)
        {
            ///////////////////////////////////////////////////////////////////////////////////////
            // Den Listener anlegen...
            final GpioPinListenerDigital listener  = new GpioPinListenerDigital() 
            {
                /**
                 * handleGpioPinDigitalStateChangeEvent() - Reaktion auf
                 * 
                 */
                @Override
                public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
                {
                    final GpioPin gpioPin = event.getPin();
                    final String pinName = gpioPin.getName();
                    final PinEdge pinEdge = event.getEdge();
                    // Die steigende Flanke A wird jeweils zur Messung ausgewertet.
                    // Die Drehrichtung ergibt sich dabei daras, ob B bereits High
                    // ist oder noch Low.
                    // Dieses Vorgehen setzt voraus, dass bei Auftritt der Flanke
                    // der Zusatnd der anderen Impulsfolge bekannt ist, dazu dienen
                    // die Zustandsgroessen is_A_High bzw. is_B_High.
                    // Anm.: Die Impulsfolgen A und B sind auch vertauschbar.
                    pinLabel:
                    synchronized (lock)
                    {
                        // Wenn Flanke UND INC_A, dann...
                        if ((PinEdge.RISING == pinEdge) && GPIO_INC_A_PIN_NAME.equals(pinName))
                        {
                            // Zustand von Impuls B...
                            final boolean is_B_High = (Model.this.gpio_Inc_B_Pin != null)? Model.this.gpio_Inc_B_Pin.isHigh() : true;
                            
                            //////////////////////////////////////////////////////////////
                            // Hier: Steigende Flanke und Impuls A...
                            //////////////////////////////////////////////////////////////
                            // Hier erfolgt das Zaehlen der Impulse in Model.this.phi 
                            // in Abhaengigkeit davon, welchen Zustand der Impuls B 
                            // angenommen hat...
                            Model.this.phi += (is_B_High)? -1L : +1L;
                            
                            if ((Model.this.phi % Model.MEASURING_NUMBER) == 0)
                            {
                                // Vielfaches von Model.PULS_NUMBER erreicht...
                                final Instant now = Instant.now();
                                Model.this.is_B_High[1] = Model.this.is_B_High[0];
                                Model.this.is_B_High [0] = is_B_High;
                                if (Model.this.is_B_High [0]^Model.this.is_B_High[1])
                                {
                                    // Excl. Oder: Wenn Richtungswechsel, dann 
                                    Model.this.lapStartTime = now;
                                    Model.this.lapEndTime = now;
                                }
                                else
                                {
                                    Model.this.lapStartTime = Model.this.lapEndTime;
                                    Model.this.lapEndTime = now;
                                }    
                            }
                            
                            break pinLabel;
                        }
                        
                        if ((PinEdge.RISING == pinEdge) && GPIO_NE555_PIN_NAME.equals(pinName))
                        {
                            ///////////////////////////////////////////////////////////////////
                            // Die Taktung hat einen Referenzpunkt erreicht.
                            ///////////////////////////////////////////////////////////////////
                            Model.this.counter++;
                            try
                            {
                                
                                int fault = Model.this.drv8830.getFault(); 
                                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                                if (fault != 0)
                                {
                                    final DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                                    logger.error("DRV8830-Fehler: " + error.getReason());
                                }
                                Model.this.drv8830.drive(drvSetPoint);
                            } 
                            catch (IOException exception)
                            {
                                logger.error("drive():", exception);
                            }
                            
                            // now zur zeitlichen Einordnung des Ereignisses...
                            // Jetzt werden die Kenngroesse der Taktung ermittelt:
                            // - now: der jetzige Zeitpunkt, die Zeitdauer ergibt sich
                            //        durch Differenzbildung zu Model.this.past...
                            // now wird im Verlauf im Zustand Model.this.past abgelegt. 
                            final Instant now = Instant.now();
                            // Model.this.past: Zeitpunkt der letzten Taktung...
                            if (Model.this.past == null)
                            {
                                // Erste Beauftragung: Model.this.past = null...
                                Model.this.past = now;
                            }
                            // Model.this.cycleTime: Taktzeit aus der Differenz now - past.
                            // Ablage der aktuell gemessenen Taktzeit in der Zustandsgroesse cycleTime...
                            Model.this.cycleTime = Duration.between(Model.this.past, now);
                          
                            // Bestimmung des Anzeigewertes von Model.this.cycleTime in Sekunden...
                            // cycleTimeDecimal - momentane Taktzeit (cycleTime) in Sekunden...
                            Model.this.cycleTimeDecimal = toBigDecimalSeconds(Model.this.cycleTime, SCALE_CYCLE_TIME);
                          
                            //////////////////////////////////////////////////////////////////////////
                            // ...und Ablage der aktuelle ermittelten Taktzeit...
                            Model.this.past = now;
                            //////////////////////////////////////////////////////////////////////////
                         
                            // Das Array position[] dient der Ermittlung des Zuwachses der Position
                            // waehrend der letzten Taktung: phi[k+1]-phi[k].
                            Model.this.position[1] = Model.this.position[0];
                            Model.this.position[0] = Model.this.phi;
                          
                            Model.this.rotation = BigDecimal.valueOf(Model.this.phi).divide(CIRCUMFERENCE, SCALE_RPM, BigDecimal.ROUND_HALF_UP);
                          
                            //////////////////////////////////////////////////////////////////////////
                            // delta - Zuwachs an Lage in Impulsen gemessen...
                            final long delta = Model.this.position[0] - Model.this.position[1];
                            int index = deltas.length-1;
                            while (index > 0)
                            {
                                Model.this.deltas[index] = Model.this.deltas[--index];
                            }
                            Model.this.deltas[0] = delta;
                            final StringBuilder logMsg = new StringBuilder();
                            for (index = 0; index < Model.this.deltas.length; index++)
                            {
                                logMsg.append(Model.this.deltas[index]);
                                logMsg.append(" ");
                            }
                            logger.debug(logMsg.toString());
                            //////////////////////////////////////////////////////////////////////////
                            
                            // increment: Zuwachs an Impulsen als BigDecimal
                            final BigDecimal increment = BigDecimal.valueOf(delta);
                          
                            Model.this.rpm = (BigDecimal.ZERO.compareTo(cycleTimeDecimal) != 0)? (increment.divide(cycleTimeDecimal, SCALE_INTERN, BigDecimal.ROUND_HALF_UP).multiply(Model.CONST).setScale(SCALE_RPM, BigDecimal.ROUND_HALF_UP)) : BigDecimal.ZERO;
                          
                            Model.this.rpm = (Model.this.rpm.abs().compareTo(BigDecimal.ONE.movePointLeft(2)) < 0)? BigDecimal.ZERO : Model.this.rpm; 
                          
                            //////////////////////////////////////////////////////////////////////////
                            // Ermittlung der Dauer einer Umdrehung
                            //
                            label:
                            {
                                for (long delta_phi: Model.this.deltas)
                                {
                                    if (delta_phi != 0L)
                                    {
                                        break label;
                                    }
                                }
                                Model.this.lapEndTime = Model.this.lapStartTime = Instant.now();
                            }
                            
                            final Duration duration  = Duration.between(Model.this.lapStartTime, Model.this.lapEndTime);
                            
                            // Zeitdauer fuer eine Umdrehung bestimmen...
                            Model.this.lapTime = MEASURING_FACTOR.multiply(toBigDecimalSeconds(duration, SCALE_LAP_TIME));
                            
                            if (Model.this.dataMap.containsKey(DATA_KEY))
                            {
                                // Die dataMap haelt die Daten zur Anzeige in der View...
                                final Object oldValue = Model.this.dataMap.get(DATA_KEY);
                                final Data oldData = (oldValue instanceof Data)? (Data) oldValue : new Data();
                              
                                // Model.this.counter: fortlaufender Zaehler...
                                final Data newData = new Data(Long.valueOf(Model.this.counter), 
                                                              Long.valueOf(Model.this.phi),
                                                              Model.this.rotation,
                                                              Model.this.lapTime,
                                                              Model.this.rpm,
                                                              Model.this.cycleTimeDecimal);
                                setProperty(DATA_KEY, newData);
                            }
                          
                            // logger.debug(now + ": Taktzeit=" + Model.this.cycleTime + ", phi=" + Model.this.phi);
                        }
                    } // 
                }
        
                /**
                 * toBigDecimalSeconds(Duration duration) - liefert die Anzahl der Sekunden
                 * <p>
                 * Vgl. toBigDecimalSeconds() aus Duration in Java 11.
                 * </p>
                 * @param duration
                 * @return
                 */
                private BigDecimal toBigDecimalSeconds(Duration duration, int scale)
                {
                    Objects.requireNonNull(duration, "duration must not be null!");
                    final BigDecimal result = BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9)).setScale(scale,  BigDecimal.ROUND_HALF_UP);
                    return (result.compareTo(BigDecimal.ONE.movePointLeft(scale)) < 0)? BigDecimal.ZERO : result;   
                }
            };
            
            
            GpioPinDigitalInput[] gpioPins = new GpioPinDigitalInput[]
            {
                this.gpioController.provisionDigitalInputPin(GPIO_NE555_PIN, GPIO_NE555_PIN_NAME, Model.PIN_PULL_RESISTANCE),                    
                this.gpioController.provisionDigitalInputPin(GPIO_INC_A_PIN, GPIO_INC_A_PIN_NAME, Model.PIN_PULL_RESISTANCE)
            };

            this.gpioController.addListener(listener, gpioPins);            
            
            ///////////////////////////////////////////////////////////////////////////////////////
            
            this.dataMap.put(DATA_KEY, new Data());
            logger.debug(DATA_KEY + " in dataMap aufgenommen.");
            
            ///////////////////////////////////////////////////////////////////////////////////////
            // Die I2C-Schnittstelle einrichten...
            try
            {
                final I2CBus i2cBus = I2CFactory.getInstance(I2CBus.BUS_1);
                this.drv8830 = new DRV8830(i2cBus.getDevice(ADDRESS));
                int fault = this.drv8830.getFault(); 
                logger.info("drv8830 liefert mit getFault() die Kennung: " + fault);                  
            } 
            catch (Throwable exception)
            {
                logger.error("I2CFactory.getInstance()", exception);
                System.exit(0);
            }
            ///////////////////////////////////////////////////////////////////////////////////////
            
        } // end if(this.isRaspi).
        else
        {
            this.dataMap.put(DATA_KEY, null);
            logger.debug(DATA_KEY + " in dataMap mit value=null aufgenommen.");
        }
    }
     
    /**
     * 
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.support.addPropertyChangeListener(listener);
    }

    /**
     * 
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.support.removePropertyChangeListener(listener);
    }

    /**
     * setProperty(String key, Object newValue) - Die View wird informiert...
     * 
     * @param key
     * @param newValue
     */
    public void setProperty(String key, Object newValue)
    {
        if (this.dataMap.containsKey(key))
        {
            Object oldValue = this.dataMap.get(key);
            
            this.dataMap.put(key, newValue);
            
            if (Model.DATA_SET_POINT_KEY.equals(key))
            {
                if (Model.this.setPointsMap.containsKey(newValue))
                {    
                    final Integer setPoint = Model.this.setPointsMap.get(newValue);
                    this.drvSetPoint = (setPoint != null)? setPoint.intValue() : 0;
                    logger.debug("drvSetPoint: " + this.drvSetPoint);
                }
            }

//            if (oldValue == null || newValue == null || !oldValue.equals(newValue))
//            {
//                logger.debug(key + ": " + oldValue + " => " + newValue);
//            }
            
            support.firePropertyChange(key, oldValue, newValue);
        }
    }
    
    /**
     * shutdown()...
     * <p>
     * Der gpioController wird auf dem Raspi heruntergefahren...
     * </p>
     */
    public void shutdown()
    {
       logger.debug("shutdown()..."); 
       if (isRaspi)
       {
           this.gpioController.shutdown();  
       }
    }
    
    /**
     * 
     */
    public void reset()
    {
        logger.debug("reset()...");
        
        this.counter = 0L;
        this.phi = 0L;
        for (int index = 0; index < this.position.length; index++)
        {
            this.position[index] = 0L;                
        }
    }

    /**
     * stop() 
     */
    public void stop()
    {
        logger.debug("stop()...");
        
        this.counter = 0L;
        this.phi = 0L;
        
        // Variablen zur Laufzeitbestimmung zuruecksetzen...
        this.lapEndTime = this.lapStartTime = Instant.now();
        
        for (int index = 0; index < this.position.length; index++)
        {
            this.position[index] = 0L;                
        }
        //
        setProperty(Model.DATA_SET_POINT_KEY, SET_POINTS[MAX_VALUE]);
        
        if (isRaspi)
        {
            try
            {
                // Abbremsen...
                this.drv8830.brake();
            
                int fault = this.drv8830.getFault(); 
                // Bei fault == 0 => Fehlerfrei, sonst Fehler!
                if (fault != 0)
                {
                    DRV8830.Fault error = DRV8830.Fault.getFault(fault);
                    logger.error("stop(): Nach brake() " + error.getReason());
                }
            }
            catch (IOException exception)
            {
                logger.error("brake():", exception);
            }
        }
    }
    
    
    @Override
    public String toString()
    {
        return "gui.Model";
    }
}
