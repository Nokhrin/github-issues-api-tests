package org.nokhrin.github.config;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class HttpClientFactory {

    private static final OkHttpClient BASE_CLIENT = new OkHttpClient.Builder().build();

    private HttpClientFactory() {
    }

    public static OkHttpClient create(Interceptor... interceptors) {
        OkHttpClient.Builder clientBuilder = BASE_CLIENT.newBuilder();
        for (Interceptor interceptor : interceptors) {
            clientBuilder.addInterceptor(interceptor);
        }
        return clientBuilder.build();
    }

    public static void shutdown() {
        BASE_CLIENT.dispatcher().cancelAll(); // отмена текущих http вызовов

        ExecutorService executorService = BASE_CLIENT.dispatcher().executorService();
        executorService.shutdown(); // запрет добавления задач

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) { // ожидание завершения задач
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            BASE_CLIENT.connectionPool().evictAll(); // закрытие сокетов
        }
    }
}
