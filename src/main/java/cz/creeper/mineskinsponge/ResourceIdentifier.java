package cz.creeper.mineskinsponge;

import com.sun.istack.internal.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.net.URI;
import java.util.Comparator;

@Getter
@EqualsAndHashCode
public class ResourceIdentifier implements Comparable<ResourceIdentifier> {
    @NonNull
    private final URI uri;

    @NonNull
    private final String version;

    public ResourceIdentifier(URI uri, String version) {
        if(!version.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format");

        this.uri = uri.normalize();
        this.version = version;
    }

    @Override
    public int compareTo(ResourceIdentifier that) {
        String[] thisParts = this.version.split("\\.");
        String[] thatParts = that.version.split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }
}
