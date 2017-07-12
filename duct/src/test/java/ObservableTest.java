import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;


/**
 * This class exists as a simplified version of what the {@link com.salesforce.pyplyn.duct.etl.configuration.TaskManager}
 *   does to process {@link com.salesforce.pyplyn.configuration.Configuration}s
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class ObservableTest {
    @Test
    public void testObservable() throws Exception {
        // ARRANGE
        final CountDownLatch DONE = new CountDownLatch(1);

        // init source objects
        Observable<List<List<String>>> src1 = Observable.just(makeMatrix(Arrays.asList("src11")));
        Observable<List<List<String>>> src2 = Observable.just(makeMatrix(Arrays.asList("src21", "src22")));
        Observable<List<List<String>>> src3 = Observable.just(makeMatrix(Arrays.asList("src31", "src32", "src33")));
        Observable<List<List<String>>> src4 = Observable.just(makeMatrix(Arrays.asList("src41"), Arrays.asList("src51")));

        // prepare transformations and processor
        List<Transform> transforms = Arrays.asList(new Transform(1, 100), new Transform(2, 0));
        Processor processor = spy(new Processor());


        // ACT

        // Concat sources
        Observable<List<List<String>>> stage = Observable.merge(src1, src2, src3, src4);

        // Merge individual into matrix
        final List<List<String>> collector = new ArrayList<>();
        Single<List<List<String>>> combinedData = stage.reduce(collector, (list, items) -> {
            list.addAll(items);
            return list;
        });

        // Transform
        stage = combinedData.toObservable();
        for (Transform t : transforms) {
            stage = stage.flatMap(t::applyAsync);
        }

        // Process
        stage.doOnComplete(DONE::countDown)
                .subscribeOn(Schedulers.computation())
                .subscribe(o -> System.out.println(processor.printList(o)));

        // wait for processing to complete
        DONE.await();


        // ASSERT

        // The sources should be combined in a single matrix
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<List<String>>> resultCaptor = ArgumentCaptor.forClass(List.class);

        verify(processor, times(1)).printList(resultCaptor.capture());
        List<List<String>> resultMatrix = resultCaptor.getValue();

        // result matrix should not be null and all transformations should be applied in order (T1, T2, etc.)
        assertThat(resultMatrix, notNullValue());
        assertThat(resultMatrix.stream().flatMap(Collection::stream).collect(Collectors.toList()), everyItem(containsString("T1-T2")));
        assertThat(resultMatrix, not(hasItem(hasItem(containsString("T2-T1")))));
   }


    private List<List<String>> makeMatrix(List<String> items) {
        return Collections.singletonList(items);
    }

    private List<List<String>> makeMatrix(List<String> items, List<String> moreItems) {
        return Arrays.asList(items, moreItems);
    }

    static class Processor {
        String printList(List<List<String>> input) {
            return input.stream().map(rows -> rows.stream().collect(Collectors.joining(" | ")))
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    static class Transform {
        final int n;
        private final int delay;

        Transform(int n, int delay) {
            this.n = n;
            this.delay = delay;
        }

        private Observable<List<List<String>>> applyAsync(List<List<String>> input) {
            return Observable.just(input).map(this::apply).delay(delay, TimeUnit.MILLISECONDS);
        }

        private List<List<String>> apply(List<List<String>> input) {
            return input.stream()
                    .map(row -> row.stream()
                            .map(this::transform)
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList());
        }

        private String transform(String input) {
            return input + "-T" + n;
        }
    }
}
