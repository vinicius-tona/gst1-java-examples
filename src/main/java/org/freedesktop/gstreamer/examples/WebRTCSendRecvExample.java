/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
 *
 * Copying and distribution of this file, with or without modification, are permitted in any medium
 * without royalty provided the copyright notice and this notice are preserved. This file is offered
 * as-is, without any warranty.
 *
 */
package org.freedesktop.gstreamer.examples;

import org.freedesktop.gstreamer.Closure;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Promise;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.lowlevel.GstAPI.GstCallback;

/**
 * A simple WebRTC example.
 */
public class WebRTCSendRecvExample
{

  public static void main(final String[] args)
  {
    System.out.println("Initializing Gst");
    Gst.init("SendRecv", new String[] {
        "--gst-debug-level=2",
        "--gst-debug-no-color"
    });

    System.out.println("Creating Pipeline");
    final String description = "webrtcbin name=sendrecv "
        + "audiotestsrc wave=red-noise ! audioconvert ! audioresample ! queue ! opusenc ! rtpopuspay ! "
        + "queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=97 ! sendrecv.";
    final Pipeline pipeline = Pipeline.launch(description);

    System.out.println("Fetching webrtcbin");
    final Element webrtcbin = pipeline.getElementByName("sendrecv");

    System.out.println("Setting on-negotiation-needed");
    /*
     * This is the gstwebrtc entry point where we create the offer and so on. It will be called when
     * the pipeline goes to PLAYING.
     */
    webrtcbin.connect("on-negotiation-needed", new NegotiationClosure());

    System.out.println("Setting on-ice-candidate");
    /*
     * We need to transmit this ICE candidate to the browser via the websockets signalling server.
     * Incoming ice candidates from the browser need to be added by us too, see on_server_message()
     */
    webrtcbin.connect("on-ice-candidate", new IceClosure());

    System.out.println("Setting pad-added");
    /* Incoming streams will be exposed via this signal */
    webrtcbin.connect("pad-added", new StreamClosure());

    System.out.println("Pipeline play!\n\n");
    pipeline.play();
  }

  public static class NegotiationClosure implements Closure
  {
    public void invoke(final Element element)
    {
      System.out.println(String.format("on-negotiation-needed called with element [%s]", element));

      final GstCallback callback = new GstCallback()
      {
        @SuppressWarnings("unused")
        public void callback(final Promise promise)
        {
          System.out.println(String.format("Callback called with promise [%s]", promise));
          promise.waitResult();
          System.out.println(String.format("Waited, getting reply..."));
          final Structure reply = promise.getReply();
          System.out.println(String.format("Reply response [%s]", reply));
        }
      };
      System.out.println("Creating promise");
      final Promise promise = new Promise(callback);

      System.out.println("Sending create-offer");
      element.emit("create-offer", null, promise);
    }
  }

  public static class IceClosure implements Closure
  {
    public void invoke(final Element element, final Integer line, final String candidate)
    {
      System.out.println(
          String.format("on-ice-candidate called!! [%s] [%s] [%s]", element, line, candidate));
    }
  }

  public static class StreamClosure implements Closure
  {
    public void invoke(final Element element, final Pad pad)
    {
      System.out.println(String.format("PadAdded called!! [%s] [%s]", element, pad));
    }
  }
}
