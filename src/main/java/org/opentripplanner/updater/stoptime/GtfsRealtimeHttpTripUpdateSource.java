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
        LOG.info("GtfsRealtimeHttpTripUpdateSource() - configure(): ");
        String url = config.path("url").asText();
        LOG.info("GtfsRealtimeHttpTripUpdateSource() - configure(): url: " +url);
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        this.feedId = config.path("feedId").asText();
        LOG.info("GtfsRealtimeHttpTripUpdateSource() - configure(): feedId: " +feedId);
    }

    @Override
    public List<TripUpdate> getUpdates() {
        LOG.info("GtfsRealtimeHttpTripUpdateSource() - getUpdates()");
        FeedMessage feedMessage = null;
        List<FeedEntity> feedEntityList = null;
        List<TripUpdate> updates = null;
        fullDataset = true;
        try {
            InputStream is = HttpUtils.getData(url);
            if (is != null) {

                LOG.info("GtfsRealtimeHttpTripUpdateSource() - getUpdates() - isNotNull");

                // Decode message
                feedMessage = FeedMessage.PARSER.parseFrom(is);

                LOG.info("feedMessage: " +feedMessage);

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
                    LOG.info("feedEntity: " +feedEntity +" hasUpdate: " +feedEntity.hasTripUpdate());
                    if (feedEntity.hasTripUpdate()) updates.add(feedEntity.getTripUpdate());
                }
            }else{
                LOG.info("GtfsRealtimeHttpTripUpdateSource() - getUpdates() - isNull !!!!!!!!!!!");
            }

        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
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
