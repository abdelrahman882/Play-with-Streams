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
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.URI;
import java.util.Date;
import java.io.BufferedReader;
import java.io.FileReader;
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
        getLocIDs();
            logic();
        start.setVisible(false);

    }

/*
● Number of Trips per day.
● Average Vehicles per day.
● Number of trips without drop-off location id for each taxi type.
● Minutes per trip for each taxi type.
● Number of trips picked up from “Madison,Brooklyn” per day for each taxi type.
* */
@FXML
    Button start;
@FXML
private NumberAxis xAxis;
    //@FXML
    //private NumberAxis yAxisL;
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



    private HashMap<Integer,OneYear> numberOfTripsPerDay = new HashMap<Integer, OneYear>();
    private int numberOfSamples=0;
    private float avgVehPerDay=0;
    private int numberOfTripsWithoutDropOffLoc [] = new int[3];
    private int minutesPerTrip [] = new int[3];
    private int minutesPerTripSamplesNum [] = new int[3];
    private int numberOfPickupsFromAB [] = new int[3]; //[type][boork or mad]
    private static final String locA = "Brooklyn";
    private static final String locB= "Madison";
    private List<Integer> listA = new ArrayList<>();
    private HashMap<String,XYChart.Data<String, Number>> cashedDays=new HashMap<>();
    private class OneYear{
            private int days [] = new int[366];
        private int vDays [] = new int[366];
        private ArrayList<String> vendors [] = new ArrayList[366];
        int currentDay=0;
            public void addTrip(int day,boolean hasDropLocation , String taxiType,int timeInMins ,int pickupLocID,String vendorID,String dayFormat){
                calcAvg(day,vendorID,dayFormat);
                calcTripsWithoutDropoffloc(hasDropLocation,taxiType);
                calcAvgTimePerTrip(timeInMins,taxiType);
                if(pickupLocID !=-1)
                calcPickupLoc(pickupLocID,taxiType);

            }
            private boolean isLoc(int id){


                    if(Collections.binarySearch(listA,id)==0){

                        return true;

                    }


                return false;

            }
            private void calcPickupLoc(int id,String type){
                if(isLoc(id)){
                    numberOfPickupsFromAB[getIndex(type)]++;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {

                            tabpdS.get(0).getData().get(getIndex(type)).setYValue(numberOfPickupsFromAB[getIndex(type)]);
                        }
                    });
                }
            }
            private int ctr[]=new int[3];
            private void calcAvgTimePerTrip(int t, String type){
                int index = getIndex(type);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        mptS.get(index).getData().add(new XYChart.Data<>(ctr[index],t));
                        if(mptS.get(index).getData().size()>=210){
                            mptS.get(index).getData().remove(0);
                            xAxis.setLowerBound(ctr[0]-200);
                            xAxis.setUpperBound(ctr[0]+20);
                            //yAxisL.setUpperBound();


                        }
                    }
                });
                ctr[index]+=1;

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
            public void closeYear(){

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
                File f = new File("src/sample/taxi_zones_simple.csv");
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    switch (values[2].replaceAll("\"","")){
                        case locA:
                            if(values[1].replaceAll("\"","").equals(locB))
                            listA.add(Integer.valueOf(values[0]));
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
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    private void processMessage(JSONObject obj){
            try {
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

                int DOY = calendar.get(Calendar.DAY_OF_YEAR);
                int year =calendar.get(Calendar.YEAR);
                if(!numberOfTripsPerDay.containsKey(year)){
                    if(numberOfTripsPerDay.containsKey(year-1)){
                        numberOfTripsPerDay.get(year-1).closeYear();
                    }
                    numberOfTripsPerDay.put(year,new OneYear());
                }
                String picLOcId =obj.getString("pickupLocationId").replaceAll("\"","");
                int val=-1;
                if (!picLOcId.equals( "")) {
                    val = Integer.valueOf(picLOcId);
                }
                numberOfTripsPerDay.get(year).addTrip(
                        DOY,
                        !dropLoc.equals(""),
                        obj.getString("taxiType"),
                        mins,
                       val,
                        obj.getString("vendorId"),
                        calendar.get(Calendar.DAY_OF_MONTH)+"/"+calendar.get(Calendar.MONTH)+"/"+calendar.get(Calendar.YEAR));
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    private  void logic () {
            try {
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
                // open websocket
                 clientEndPoint = new ClientEP(new URI("ws://localhost:9000/ws"));

                // add listener
                clientEndPoint.addMessageHandler(new MessageRec() {
                    @Override
                    public void handleMessage(String message) {
                        try {
                            processMessage(new JSONObject(message));
                        }catch (Exception e){
                            //TODO
                        }

                    }
                } );
                // wait 5 seconds for messages from websocket
                Thread.sleep(10000);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

}
