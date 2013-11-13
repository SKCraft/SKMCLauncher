package com.sk89q.skmcl.worker;

import lombok.Getter;

import java.util.Observable;
import java.util.Observer;

public class Segment extends Observable implements Watchable, Observer {

    @Getter
    private double offset;
    @Getter
    private final double percentage;
    @Getter
    private final double interval;

    @Getter
    private double total;

    private double progress = -1;
    private String localizedStatus;
    private String localizedTitle;
    private boolean shouldConfirmInterrupt = false;

    public Segment() {
        this.offset = 0;
        this.percentage = 0;
        this.interval = 0;
    }

    private Segment(double offset, double percentage, double interval) {
        this.offset = offset;
        this.percentage = percentage;
        this.interval = interval;
    }

    public synchronized Segment segment(double percentage) {
        Segment segment = new Segment(total, percentage, 0);
        total += percentage;
        segment.addObserver(this);
        return segment;
    }

    public synchronized Segment segments(double totalPercentage, double count) {
        Segment segment = new Segment(
                total, totalPercentage / count, totalPercentage / count);
        total += totalPercentage;
        segment.addObserver(this);
        return segment;
    }

    public void advance() {
        offset += interval;
        deleteObservers();
    }

    @Override
    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
        setChanged();
        notifyObservers();
    }

    @Override
    public String getLocalizedStatus() {
        return localizedStatus;
    }

    public void setLocalizedStatus(String localizedStatus) {
        this.localizedStatus = localizedStatus;
        setChanged();
        notifyObservers();
    }

    public void push(double progress, String localizedStatus) {
        this.progress = progress;
        this.localizedStatus = localizedStatus;
        setChanged();
        notifyObservers();
    }

    @Override
    public String getLocalizedTitle() {
        return localizedTitle;
    }

    public void setLocalizedTitle(String localizedTitle) {
        this.localizedTitle = localizedTitle;
        setChanged();
        notifyObservers();
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        return shouldConfirmInterrupt;
    }

    public void setShouldConfirmInterrupt(boolean shouldConfirmInterrupt) {
        this.shouldConfirmInterrupt = shouldConfirmInterrupt;
        setChanged();
        notifyObservers();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof Watchable) {
            Watchable watchable = (Watchable) o;
            Segment segment;

            double progress = watchable.getProgress();

            if (progress >= 0) {
                if (watchable instanceof Segment &&
                        (segment = (Segment) watchable).getPercentage() > 0) {
                    progress = (watchable.getProgress() * segment.getPercentage() + segment.getOffset()) / getTotal();
                }

                this.progress = progress;
            }

            this.localizedStatus = watchable.getLocalizedStatus();
            this.localizedTitle = watchable.getLocalizedTitle();
            this.shouldConfirmInterrupt = watchable.shouldConfirmInterrupt();

            setChanged();
            notifyObservers();
        }
    }
}
