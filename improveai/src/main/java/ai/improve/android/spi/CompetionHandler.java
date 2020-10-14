package ai.improve.android.spi;

public interface CompetionHandler {


    void onError(String error);

    void onSuccess(String success);
}
