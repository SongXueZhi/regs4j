package model;

import java.util.HashMap;
import java.util.Map;

public class TestFile extends ChangedFile {
	public Type type;
	private String qualityClassName;

	
	public TestFile(String newPath) {
		super(newPath);
	}

	public String getQualityClassName() {
		return qualityClassName;
	}

	public void setQualityClassName(String qualityClassName) {
		this.qualityClassName = qualityClassName;
	}



}
