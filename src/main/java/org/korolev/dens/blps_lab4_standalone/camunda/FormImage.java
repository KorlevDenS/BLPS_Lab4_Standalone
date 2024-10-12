package org.korolev.dens.blps_lab4_standalone.camunda;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class FormImage implements MultipartFile {

    private final byte[] content;

    public FormImage(@Nullable ByteArrayInputStream inputStream) {
        if (inputStream == null) {
            this.content = new byte[0];
        } else {
            this.content = inputStream.readAllBytes();
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "anonymous_picture";
    }

    @Override
    public String getOriginalFilename() {
        return "anonymous_picture";
    }

    @Override
    public String getContentType() {
        return "img/png";
    }

    @Override
    public boolean isEmpty() {
        return this.content.length == 0;
    }

    @Override
    public long getSize() {
        return this.content.length;
    }

    @Nonnull
    @Override
    public byte[] getBytes() {
        return this.content;
    }

    @Nonnull
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@Nonnull File dest) throws IOException, IllegalStateException  {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }
}
