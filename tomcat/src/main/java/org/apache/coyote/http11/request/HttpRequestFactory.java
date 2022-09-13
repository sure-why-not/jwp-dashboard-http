package org.apache.coyote.http11.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpRequestFactory {

    private static final String REQUEST_LINE_SPLITTER = " ";
    private static final int METHOD_INDEX = 0;
    private static final int URI_INDEX = 1;
    private static final int PROTOCOL_INDEX = 2;
    private static final String END_OF_REQUEST_HEADER = "";
    private static final String REQUEST_HEADER_DELIMITER = ": ";
    private static final int REQUEST_HEADER_FIELD_INDEX = 0;
    private static final int REQUEST_HEADER_VALUE_INDEX = 1;
    private static final String CONTENT_LENGTH_KEY = "Content-Length";

    public static HttpRequest create(BufferedReader reader) {
        try {
            String line = reader.readLine();
            RequestLine requestLine = parseToRequestLine(line);
            RequestHeaders requestHeaders = parseToRequestHeaders(reader);
            return new HttpRequest(requestLine, requestHeaders, parseToRequestBody(reader, requestHeaders));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RequestLine parseToRequestLine(String line) {
        String[] requestLineContents = line.split(REQUEST_LINE_SPLITTER);
        Method method = Method.from(requestLineContents[METHOD_INDEX]);
        URI uri = new URI(requestLineContents[URI_INDEX]);
        String protocol = requestLineContents[PROTOCOL_INDEX];

        return new RequestLine(method, uri, protocol);
    }

    private static RequestHeaders parseToRequestHeaders(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        Map<String, String> headers = new HashMap<>();

        while (!line.equals(END_OF_REQUEST_HEADER)) {
            String[] headerContents = line.split(REQUEST_HEADER_DELIMITER);
            headers.put(headerContents[REQUEST_HEADER_FIELD_INDEX], headerContents[REQUEST_HEADER_VALUE_INDEX]);
            line = reader.readLine();
        }
        return new RequestHeaders(headers);
    }

    private static RequestBody parseToRequestBody(BufferedReader reader, RequestHeaders requestHeaders)
            throws IOException {
        Optional<String> contentLengthValue = requestHeaders.getHeaderValue(CONTENT_LENGTH_KEY);
        if (contentLengthValue.isEmpty()) {
            return RequestBody.ofEmpty();
        }
        int contentLength = Integer.parseInt(contentLengthValue.get());
        String requestBodyValue = parseToRequestBodyValue(reader, contentLength);

        return RequestBody.of(requestBodyValue);
    }

    private static String parseToRequestBodyValue(BufferedReader reader, int contentLength) throws IOException {
        char[] buffer = new char[contentLength];
        reader.read(buffer, 0, contentLength);
        return new String(buffer);
    }
}
