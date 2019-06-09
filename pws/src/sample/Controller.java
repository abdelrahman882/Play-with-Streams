package sample;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.json.*;
import javafx.application.Platform;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.URI;
import java.util.Date;

import javafx.scene.chart.XYChart;
public class Controller {
    private static Controller inst=null;
    public  static  Controller getInstance(){
        if(inst==null){
            return  new Controller();
        }else {
            return inst;
        }
    }
    public Controller(){


    }
    public void getData(){

        start.setText("loading..");

        if(logic()){
            start.setVisible(false);
            getLocIDs();
            setUI();
        }else {
            start.setText("try Again");
        }


    }


    @FXML
    Button start;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxisx;
    @FXML
    private BarChart<String, Number> twdf;

    @FXML
    private BarChart<String, Number> tpd;

    @FXML
    private LineChart<Number, Number> vpd;

    @FXML
    private LineChart<Number, Number> mpt;

    @FXML
    private BarChart<String, Number> tabpd;

    private ClientEP clientEndPoint;
    private XYChart.Series<String,Number> tripsPerDay=new XYChart.Series<>();
    private XYChart.Series<Number,Number> vendorsData=new XYChart.Series<>();
    private ArrayList<XYChart.Series<String,Number>> twdfS = new ArrayList<XYChart.Series<String,Number>>();
    private ArrayList<XYChart.Series<Number,Number>> mptS = new ArrayList<XYChart.Series<Number,Number>>();
    private ArrayList<XYChart.Series<String,Number>> tabpdS = new ArrayList<XYChart.Series<String,Number>>();
    private HashMap<Integer,Month> numberOfTripsPerDay = new HashMap<Integer,Month>();
    private int numberOfSamples=0;
    private float avgVehPerDay=0;
    private  int numberOfRecords =0;
    private  int numberOfTrips =0;
    private int numberOfTripsWithoutDropOffLoc [] = new int[3];
    private int minutesPerTrip [] = new int[3];
    private int minutesPerTripSamplesNum [] = new int[3];
    private int numberOfPickupsFromAB [] = new int[3]; //[type][boork or mad]
    private static final String locA = "Brooklyn";
    private static final String locA2= "Madison";
    private static final String locB= "Queens";
    private static final String locB2= "Woodside";
    private List<Integer> listA = new ArrayList<>();
    private List<Integer> listB = new ArrayList<>();

    private HashMap<String,XYChart.Data<String, Number>> cashedDays=new HashMap<>();
    private class Month{
        private int days [] = new int[31];
        private int vDays [] = new int[31];

        private ArrayList<String> vendors [] = new ArrayList[31];
        private HashSet<String> allVendors  = new HashSet();
        private void updateTxtFile(){
            StringBuilder res = new StringBuilder();
            res.append(numberOfRecords+","
                    +numberOfTrips+","
                    +getAvgTripsPerDay()+","
                    +allVendors.size()+","
            +numberOfPickupsFromCD[0]+","
            +numberOfPickupsFromCD[1]+","
            +numberOfPickupsFromCD[2]);


            try {
                    PrintWriter writer ;
                    writer = new PrintWriter("statistics.txt", "UTF-8");
                writer.println(res.toString());
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        public void addTrip(int day,boolean hasDropLocation , String taxiType,int timeInMins ,int pickupLocID,String vendorID,String dayFormat){
            if(vendorID!=null&&vendorID!=""&&!allVendors.contains(vendorID))    {
                allVendors.add(vendorID);
            }
            calcAvg(day-1,vendorID,dayFormat);
            calcTripsWithoutDropoffloc(hasDropLocation,taxiType);
            calcAvgTimePerTrip(timeInMins,taxiType);
            if(pickupLocID !=-1)
                calcPickupLoc(pickupLocID,taxiType);

            updateTxtFile();

        }
        private float getAvgTripsPerDay(){
            int sum = 0;
            int numberOfDays=0;
            for(int i=0;i<days.length;i++){
                if(days[i]!=0)numberOfDays++;
                sum += days[i];
            }
            return sum/numberOfDays;
        }
        private boolean isLoc(int id, int from){
            if(from==0) {
                if (Collections.binarySearch(listA, id) == 0) {
                    return true;
                }
            }else {

                if (Collections.binarySearch(listB, id) == 0) {
                    return true;
                }
            }
            return false;
        }
        int numberOfPickupsFromCD [] = new int [3];
        private void calcPickupLoc(int id,String type){
            if(id==149){
                System.out.println("lllllool");
            }
            if(isLoc(id,0)){  // loc a
                numberOfPickupsFromAB[getIndex(type)]++;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        tabpdS.get(0).getData().get(getIndex(type)).setYValue(numberOfPickupsFromAB[getIndex(type)]);
                    }
                });
            }
            if(isLoc(id,1)){ // loc c

                numberOfPickupsFromCD[getIndex(type)]++;
            }
        }
        private int ctr=0;

        private void calcAvgTimePerTrip(int t, String type){
            int index = (getIndex(type)+1 )%3 ;//ui and coloring issue
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    mptS.get(index).getData().add(new XYChart.Data<>(ctr,t));
                    if(mptS.get(index).getData().size()>=210){
                        mptS.get(index).getData().remove(0);
                        xAxis.setLowerBound(ctr-250);
                        xAxis.setUpperBound(ctr+20);
                    }
                }
            });

            ctr+=1;

            minutesPerTrip[index] = minutesPerTrip[index] *minutesPerTripSamplesNum[index] +t;
            minutesPerTripSamplesNum[index]++;
            minutesPerTrip[index] /= minutesPerTripSamplesNum[index]+1;
        }
        private void calcTripsWithoutDropoffloc(boolean has,String type){
            if(!has){
                numberOfTripsWithoutDropOffLoc[getIndex(type)]++;
                twdfS.get(0).getData().get(getIndex(type)).setYValue( numberOfTripsWithoutDropOffLoc[getIndex(type)]);
            }
        }
        private void calcAvg(int day,String vendorID,String dayFormat){

            if(vendors[day]==null){
                vendors[day]=new ArrayList<>();
            }
            if(!vendors[day].contains(vendorID)){
                vDays[day]++;
                vendors[day].add(vendorID);
                avgVehPerDay=avgVehPerDay * (numberOfSamples);
                if(days[day]==0) {
                    numberOfSamples++;
                }
                avgVehPerDay ++;
                avgVehPerDay /= numberOfSamples;
                if(days[day]==0){
                    vendorsData.getData().add(new XYChart.Data<Number, Number>(numberOfSamples,(int)avgVehPerDay));
                }else {
                    vendorsData.getData().get(vendorsData.getData().size()-1).setYValue((int)avgVehPerDay);
                }
            }
            days[day]++;
            if(cashedDays.containsKey(String.valueOf(day))){
                cashedDays.get(String.valueOf(day)).setYValue(days[day]);
            }else {
                if(cashedDays.size()>10){
                    XYChart.Data<String,Number> d = tripsPerDay.getData().get(0);
                    tripsPerDay.getData().remove(0);
                    cashedDays.remove(d.getXValue());
                }
                XYChart.Data<String,Number>temp =new XYChart.Data<>(dayFormat, days[day]);
                cashedDays.put(String.valueOf(day),temp);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        tripsPerDay.getData().add(temp);
                    }
                });
            }


        }
        int getIndex(String type){
            switch (type){
                case "yellow":
                    return 0;
                case "green":
                    return 1;
                case "fhv":
                    return 2;

            }
            return 0;

        }

    }
    private void getLocIDs(){


        try {
            InputStream in = getClass().getResourceAsStream("taxi_zones_simple.csv");
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
           // File f = new File("./sample/taxi_zones_simple.csv");
            //BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                switch (values[2].replaceAll("\"","")){
                    case locA:

                        if(values[1].replaceAll("\"","").equals(locA2))
                            listA.add(Integer.valueOf(values[0]));
                        break;
                    case locB:
                        if(values[1].replaceAll("\"","").equals(locB2))
                            listB.add(Integer.valueOf(values[0]));
                        break;
                    default:
                        break;
                }
            }
            Comparator<Integer>  c =new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if(o1==null)return -1;
                    if(o2==null)return 1;
                    if(o1>o2){
                        return 1;
                    }else if(o1<o2){
                        return -1;
                    }
                    return 0 ;
                }
            };
            listA.sort(c);
            listB.sort(c);
        }catch (Exception e){

            e.printStackTrace();
        }

    }
    private void processMessage(JSONObject obj){
        try {
            numberOfRecords++;
            String pickupDate = obj.getString("pickupDateTime");
            String dropoffDate = obj.getString("dropOffDatetime");
            Date date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(pickupDate.replaceAll("\"", ""));
            Date dateDrop = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dropoffDate.replaceAll("\"",""));


            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            Calendar cDrop = Calendar.getInstance();
            cDrop.setTime(dateDrop);
            int mins = (int) ((cDrop.getTimeInMillis() - calendar.getTimeInMillis())/60000);
            String dropLoc = obj.getString("dropOffLocationId").replaceAll("\"","");

            int DOM = calendar.get(Calendar.DAY_OF_MONTH);
            int month =calendar.get(Calendar.MONTH);
            if(!numberOfTripsPerDay.containsKey(month)){
                numberOfTripsPerDay.put(month,new Month());
            }
            String picLOcId =obj.getString("pickupLocationId").replaceAll("\"","");
            int val=-1;
            if (!picLOcId.equals( "")) {
                val = Integer.valueOf(picLOcId);
            }
            numberOfTripsPerDay.get(month).addTrip(
                    DOM,
                    !dropLoc.equals(""),
                    obj.getString("taxiType"),
                    mins,
                    val,
                    obj.getString("vendorId"),
                    calendar.get(Calendar.DAY_OF_MONTH)+"/"+calendar.get(Calendar.MONTH)+"/"+calendar.get(Calendar.YEAR));
            numberOfTrips++;

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    private  void setUI(){
        xAxis.setAutoRanging(false);
        tabpdS.add( new XYChart.Series<String, Number>());
        tabpdS.get(0).getData().add(new XYChart.Data<String,Number>("yellow",0));
        tabpdS.get(0).getData().add(new XYChart.Data<String,Number>("green",0));
        tabpdS.get(0).getData().add(new XYChart.Data<String,Number>("fhv",0));
        tabpdS.get(0).setName("Madison,Brooklyn");


        tabpd.getData().addAll(tabpdS.get(0));
        twdfS.add( new XYChart.Series<String, Number>());
        twdfS.get(0).getData().add(new XYChart.Data<String,Number>("yellow",0));
        twdfS.get(0).getData().add(new XYChart.Data<String,Number>("green",0));
        twdfS.get(0).getData().add(new XYChart.Data<String,Number>("fhv",0));

        twdfS.get(0).setName("  number of trips \nwithout drop-off loc");


        twdf.getData().addAll(twdfS.get(0));
        mptS.add(new XYChart.Series<>());
        mptS.get(0).setName("fhv");

        mptS.add(new XYChart.Series<>());
        mptS.get(1).setName("yellow");

        mptS.add(new XYChart.Series<>());
        mptS.get(2).setName("green");



        mpt.getData().addAll(mptS.get(0),mptS.get(1),mptS.get(2));
        mpt.setCreateSymbols(false);
        vpd.setCreateSymbols(false);
        vpd.getData().add(vendorsData);
        tpd.getData().add(tripsPerDay);
        tripsPerDay.setName("Number of Trips per day");
        vendorsData.setName("Average Vehicles per day");
        Paint yellow = Color.YELLOW;
        tpd.getYAxis().setTickLabelFill(yellow);

        vpd.getXAxis().setTickLabelFill(yellow);
        vpd.getYAxis().setTickLabelFill(yellow);

        vpd.setAnimated(false);
        tpd.setAnimated(false);
        mpt.setAnimated(false);
        tabpd.setAnimated(false);
        twdf.setAnimated(false);
    }
    private  boolean logic () {
        try {

            try {
                // open websocket
                clientEndPoint = new ClientEP(new URI("ws://localhost:9000/ws"));
            }catch (RuntimeException e){
                return  false;
            }
            // add listener
            clientEndPoint.addMessageHandler(new MessageRec() {
                @Override
                public void handleMessage(String message) {
                    try {
                        processMessage(new JSONObject(message));
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            } );

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

}
