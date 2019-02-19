package org.opentripplanner.updater.stoptime;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class GtfsRealtimeHttpTripUpdateSource implements TripUpdateSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeHttpTripUpdateSource.class);

    /**
     * True iff the last list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private boolean fullDataset = true;
    
    /**
     * Feed id that is used to match trip ids in the TripUpdates
     */
    private String feedId;

    private String url;

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        System.out.println("GtfsRealtimeHttpTripUpdateSource() - configure(): ");
        String url = config.path("url").asText();
        System.out.println("GtfsRealtimeHttpTripUpdateSource() - configure(): url: " +url);
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        this.feedId = config.path("feedId").asText();
        System.out.println("GtfsRealtimeHttpTripUpdateSource() - configure(): feedId: " +feedId);
    }

    @Override
    public List<TripUpdate> getUpdates() {
        System.out.println("GtfsRealtimeHttpTripUpdateSource() - getUpdates()");
        FeedMessage feedMessage = null;
        List<FeedEntity> feedEntityList = null;
        List<TripUpdate> updates = null;
        fullDataset = true;
        try {
            InputStream is = HttpUtils.getData(url);
            if (is != null) {

                System.out.println("GtfsRealtimeHttpTripUpdateSource() - getUpdates() - isNotNull");

                // Decode message
                feedMessage = FeedMessage.PARSER.parseFrom(is);
                feedEntityList = feedMessage.getEntityList();
                
                // Change fullDataset value if this is an incremental update
                if (feedMessage.hasHeader()
                        && feedMessage.getHeader().hasIncrementality()
                        && feedMessage.getHeader().getIncrementality()
                                .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)) {
                    fullDataset = false;
                }
                
                // Create List of TripUpdates
                updates = new ArrayList<>(feedEntityList.size());
                for (FeedEntity feedEntity : feedEntityList) {
                    if (feedEntity.hasTripUpdate()) updates.add(feedEntity.getTripUpdate());
                }
            }
            System.out.println("GtfsRealtimeHttpTripUpdateSource() - getUpdates() - isNull !!!!!!!!!!!");
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
            System.out.println("Failed to parse gtfs-rt feed from " + url + ":" +e.toString());
        }
        return updates;
    }

    @Override
    public boolean getFullDatasetValueOfLastUpdates() {
        return fullDataset;
    }
    
    public String toString() {
        return "GtfsRealtimeHttpUpdateStreamer(" + url + ")";
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }
}
