package com.github.stoton.pvmonitor_widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWidget extends AppWidgetProvider {
    private static final String URL = "http://pvmonitor.pl/widget.php?idinst=10143";
    private static final String NUMBER_REGEX = "[0-9]*";
    private static final String UPDATE_ACTION = "update";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
            RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.activity_main);
            views.setOnClickPendingIntent(R.id.button,
                    getPendingSelfIntent(context, UPDATE_ACTION));
            appWidgetManager.updateAppWidget(appWidgetIds[0], views);
            new DownloadDataFromWebsite(views, context, appWidgetIds).execute();
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (UPDATE_ACTION.equals(intent.getAction())){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context,  MainWidget.class);

            int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, AppWidgetManager.getInstance(context), ids);
        }
    }

    private class DownloadDataFromWebsite extends AsyncTask<Void, Void, Void> {
        private RemoteViews views;
        private Context context;
        private int[] appWidgetIds;
        private StringBuilder power = new StringBuilder();
        private StringBuilder consumption = new StringBuilder();
        private StringBuilder production = new StringBuilder();
        private StringBuilder efficiency = new StringBuilder();
        private StringBuilder today = new StringBuilder();
        private StringBuilder todayConsumption = new StringBuilder();
        private StringBuilder todayFromPV = new StringBuilder();
        private double todayDouble;
        private double todayConsumptionDouble;

        DownloadDataFromWebsite(RemoteViews remoteViews, Context context, int[] appWidgetIds) {
            this.views = remoteViews;
            this.context = context;
            this.appWidgetIds = appWidgetIds;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Document document = null;
            try {
                document = Jsoup.connect(URL).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String text;
            if (document != null) {
                text = document.text();

                Pattern numberPattern = Pattern.compile(NUMBER_REGEX);

                int counter = 1;
                Matcher matcher = numberPattern.matcher(text);

                int count = getMatcherSize(numberPattern, text);

                if (count == 9) {
                    while (matcher.find()) {
                        if (!matcher.group().equals("")) {
                            if (counter == 1) power.append(matcher.group());
                            if (counter == 2) consumption.append(matcher.group());
                            if (counter == 3) production.append(matcher.group());
                            if (counter == 4) efficiency.append(matcher.group());
                            if (counter == 5) efficiency.append(",").append(matcher.group());
                            if (counter == 6) {
                                today.append(matcher.group());
                                todayDouble = Double.valueOf(today.toString()) / 1000;
                            }
                            if (counter == 7) {
                                todayConsumption.append(matcher.group());
                                todayConsumptionDouble = Double.valueOf(todayConsumption.toString()) / 1000;
                            }
                            if (counter == 8) todayFromPV.append(matcher.group());
                            if (counter == 9) todayFromPV.append(",").append(matcher.group());
                            counter++;
                        }
                    }
                } else {
                    while (matcher.find()) {
                        if (!matcher.group().equals("")) {
                            if (counter == 1) power.append(matcher.group());
                            if (counter == 2) consumption.append(matcher.group());
                            if (counter == 3) production.append(matcher.group()).append(",");
                            if (counter == 4) production.append(matcher.group());
                            if (counter == 5) efficiency.append(matcher.group());
                            if (counter == 6) efficiency.append(",").append(matcher.group());
                            if (counter == 7) {
                                today.append(matcher.group());
                                todayDouble = Double.valueOf(today.toString()) / 1000;
                            }
                            if (counter == 8) {
                                todayConsumption.append(matcher.group());
                                todayConsumptionDouble = Double.valueOf(todayConsumption.toString()) / 1000;
                            }
                            if (counter == 9) todayFromPV.append(matcher.group());
                            if (counter == 10) todayFromPV.append(",").append(matcher.group());
                            counter++;
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Problemy ze stroną", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void arg) {
            views.setTextViewText(R.id.watt, power + " W");
            views.setTextViewText(R.id.effInPercentConsumption, efficiency + " %");
            views.setTextViewText(R.id.powerToday, String.valueOf(todayDouble) + " kWh");
            views.setTextViewText(R.id.consumptionWatt, consumption + " W");
            views.setTextViewText(R.id.effInPercent, production + " %");
            views.setTextViewText(R.id.todayConsumption, String.valueOf(todayConsumptionDouble) + " kWh");
            views.setTextViewText(R.id.todayFromPV, "Dziś " + todayFromPV + "% z pv");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(appWidgetIds[0], views);
        }
    }

    private int getMatcherSize(Pattern pattern,  String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);

        while(matcher.find()) {
            if(!matcher.group().equals(""))
                count++;
        }
        return count;
    }
}