package org.apache.coyote.http11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.model.User;
import org.apache.coyote.Processor;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.request.QueryParams;
import org.apache.coyote.http11.request.URI;
import org.apache.coyote.http11.response.ContentType;
import org.apache.coyote.http11.response.HttpResponse;
import org.apache.coyote.http11.response.HttpStatus;
import org.apache.coyote.util.FileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);
    private static final String WELCOME_MESSAGE = "Hello world!";

    private final Socket connection;

    public Http11Processor(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        process(connection);
    }

    @Override
    public void process(Socket connection) {
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = connection.getOutputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            HttpRequest httpRequest = new HttpRequest(reader.readLine());
            HttpResponse httpResponse = getResponse(httpRequest);
            String response = httpResponse.parseToString();

            outputStream.write(response.getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    public HttpResponse getResponse(HttpRequest httpRequest) throws IOException {
        try {
            if (httpRequest.isStaticFileRequest()) {
                return getStaticResourceResponse(httpRequest);
            }
            return getDynamicResourceResponse(httpRequest);
        } catch (RuntimeException e) {
            return new HttpResponse(httpRequest.getProtocol(), HttpStatus.NOT_FOUND,
                    ContentType.TEXT_HTML_CHARSET_UTF_8, "페이지를 찾을 수 없습니다.");
        }
    }

    private HttpResponse getStaticResourceResponse(HttpRequest httpRequest) {
        Optional<String> extension = httpRequest.getExtension();
        if (extension.isPresent()) {
            ContentType contentType = ContentType.from(extension.get());
            return new HttpResponse(httpRequest.getProtocol(), HttpStatus.OK, contentType,
                    getStaticResourceResponse(httpRequest.getUri().getPath()));
        }
        return new HttpResponse(httpRequest.getProtocol(), HttpStatus.NOT_FOUND,
                ContentType.TEXT_HTML_CHARSET_UTF_8, "페이지를 찾을 수 없습니다.");
    }

    private String getStaticResourceResponse(String resourcePath) {
        return FileReader.readStaticFile(resourcePath, this.getClass());
    }

    private HttpResponse getDynamicResourceResponse(HttpRequest httpRequest) {
        URI uri = httpRequest.getUri();
        String path = uri.getPath();
        if (path.equals("/")) {
            return new HttpResponse(httpRequest.getProtocol(), HttpStatus.OK, ContentType.TEXT_HTML_CHARSET_UTF_8,
                    WELCOME_MESSAGE);
        }
        if (path.equals("/login")) {
            QueryParams queryParams = uri.getQueryParams();
            Optional<User> user = InMemoryUserRepository.findByAccount(queryParams.findValue("account"));
            user.ifPresent(value -> log.debug(value.toString()));
        }
        String responseBody = getStaticResourceResponse(path + ".html");
        return new HttpResponse(httpRequest.getProtocol(), HttpStatus.OK, ContentType.TEXT_HTML_CHARSET_UTF_8,
                responseBody);
    }
}
