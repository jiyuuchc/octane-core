package edu.uchc.octane.core.utils;

import org.json.JSONObject;

public class TaggedImage {
   public final Object pix;
   public JSONObject tags;

   public TaggedImage(Object pix, JSONObject tags) {
      this.pix = pix;
      this.tags = tags;
   }
}
