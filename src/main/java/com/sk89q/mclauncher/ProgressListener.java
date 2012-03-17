package com.sk89q.mclauncher;

import java.util.EventListener;
import java.util.EventObject;

public interface ProgressListener extends EventListener {

    public void titleChanged(TitleChangeEvent event);
    public void statusChanged(StatusChangeEvent event);
    public void valueChanged(ValueChangeEvent event);
    public void completed(EventObject eventObject);

}
