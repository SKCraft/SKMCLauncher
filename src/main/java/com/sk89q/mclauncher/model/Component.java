package com.sk89q.mclauncher.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "component")
public class Component {
    
    private String id;
    private String title;
    private String description;
    private boolean defaultSelected;
    private boolean required;
    
    private transient boolean selected;

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAttribute(name = "default")
    public boolean isDefaultSelected() {
        return defaultSelected;
    }
    
    public void setDefaultSelected(boolean defaultSelected) {
        this.defaultSelected = defaultSelected;
        if (defaultSelected) {
            selected = true;
        }
    }

    @XmlAttribute(name = "required")
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }

    @XmlTransient
    public boolean isSelected() {
        return required || selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Override
    public String toString() {
        return getTitle();
    }

}
