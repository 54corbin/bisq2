/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.persistence;

import bisq.common.threading.ExecutorFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class Persistence<T extends PersistableStore<T>> {
    public static final ExecutorService PERSISTENCE_IO_POOL = ExecutorFactory.newFixedThreadPool("Persistence-io-pool");

    @Getter
    private final String fileName;
    @Getter
    private final String storagePath;
    private final Object lock = new Object();
    private final AtomicReference<T> candidateToPersist = new AtomicReference<>();

    private final PersistableStoreReaderWriter<T> persistableStoreReaderWriter;

    public Persistence(String directory, String fileName) {
        this.fileName = fileName;
        storagePath = directory + File.separator + fileName;

        Path storePath = Path.of(directory, fileName);
        var storeFileManager = new PersistableStoreFileManager(storePath);
        persistableStoreReaderWriter = new PersistableStoreReaderWriter<>(storeFileManager);
    }

    public CompletableFuture<Optional<T>> readAsync(Consumer<T> consumer) {
        return readAsync().whenComplete((result, throwable) -> result.ifPresent(consumer));
    }

    public CompletableFuture<Optional<T>> readAsync() {
        return CompletableFuture.supplyAsync(persistableStoreReaderWriter::read, PERSISTENCE_IO_POOL);
    }

    public CompletableFuture<Boolean> persistAsync(T serializable) {
        synchronized (lock) {
            candidateToPersist.set(serializable);
        }
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("Persistence.persist-" + fileName);
            return persist(candidateToPersist.get());
        }, PERSISTENCE_IO_POOL);
    }

    public boolean persist(T persistableStore) {
        persistableStoreReaderWriter.write(persistableStore);
        return true;
    }
}
