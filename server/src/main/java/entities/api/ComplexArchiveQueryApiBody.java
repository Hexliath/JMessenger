package entities.api;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ComplexArchiveQueryApiBody extends GenericApiEntity {

    private List<UUID> channels;
    private Date since;

    public List<UUID> getChannels() {
        return channels;
    }

    public void setChannels(List<UUID> channels) {
        this.channels = channels;
    }

    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }

    @Override
    public String toString() {
        return "ComplexArchiveQueryApiBody{" +
                "channels=" + channels +
                ", since=" + since.getTime() +
                '}';
    }
}
