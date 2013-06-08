package com.sk89q.mclauncher.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

class Setting {

    @XmlAttribute
    public String key;
    
    @XmlValue
    public String value;
    
    public Setting() {
    }

}
