package cz.creeper.mineskinsponge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

/**
 * A versioned resource with a method to open the stream.
 */
@AllArgsConstructor
@Getter
@ToString
public class Resource implements Comparable<Resource> {
    @NonNull
    private final ResourceIdentifier identifier;

    @NonNull
    private final Supplier<InputStream> inputStreamSupplier;

    public Resource(URI uri, String version, Supplier<InputStream> inputStreamSupplier) {
        this.identifier = new ResourceIdentifier(uri, version);
        this.inputStreamSupplier = inputStreamSupplier;
    }

    @Override
    public int compareTo(Resource o) {
        return identifier.compareTo(o.identifier);
    }

    public InputStream openStream() {
        return inputStreamSupplier.get();
    }
}
