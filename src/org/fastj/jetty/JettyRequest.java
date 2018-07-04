package org.fastj.jetty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.fastj.app.Args;
import org.fastj.log.LogUtil;
import org.fastj.rest.api.Request;

public class JettyRequest extends Request<org.eclipse.jetty.server.Request> {

	private List<FileItem> fitems;

	@Override
	protected String readEntity(org.eclipse.jetty.server.Request entity) throws IOException {
		return readAll(entity.getInputStream());
	}

	@Override
	public String endpoint() {
		org.eclipse.jetty.server.Request r = getEntity();
		StringBuffer buff = r.getRequestURL();
		int idx1 = buff.indexOf("://") + 3;
		return buff.substring(0, buff.indexOf("/", idx1));
	}

	@Override
	public Optional<FileItem> fileItem(String item) {
		org.eclipse.jetty.server.Request request = getEntity();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (!isMultipart) {
			return Optional.empty();
		}

		try {
			synchronized (request) {
				if (fitems == null) {
					ServletFileUpload upload = initUpload(getEncoding());
					fitems = upload.parseRequest(request);
				}
			}

			for (FileItem fi : fitems) {
				if (item.equals(fi.getFieldName())) {
					return Optional.of(fi);
				}
			}

		} catch (FileUploadException e) {
			LogUtil.error("parse upload fail", e);
		}

		return Optional.empty();
	}

	@Override
	public List<FileItem> fileItems() {
		org.eclipse.jetty.server.Request request = getEntity();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (!isMultipart) {
			return null;
		}

		try {
			synchronized (request) {
				if (fitems == null) {
					ServletFileUpload upload = initUpload(getEncoding());
					fitems = upload.parseRequest(request);
				}
			}

			return fitems;
		} catch (FileUploadException e) {
			LogUtil.error("parse upload fail", e);
		}

		return new ArrayList<>();
	}

	public Uploads saveUploads(File dir) {
		if (dir == null) {
			throw new NullPointerException("Request.saveUploads: Dir is null!");
		}

		org.eclipse.jetty.server.Request request = getEntity();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (!isMultipart) {
			return null;
		}

		Uploads ups = new Uploads();

		try {
			synchronized (request) {
				if (fitems == null) {
					ServletFileUpload upload = initUpload(getEncoding());
					fitems = upload.parseRequest(request);
				}
			}

			for (FileItem fi : fitems) {
				if (fi.isFormField()) {
					String k = fi.getFieldName();
					String v = fi.getString();
					ups.addBodys(k, v);
				} else {
					try {
						File f;
						fi.write(f = new File(dir, fi.getName()));
						ups.addFiles(f);
					} catch (Exception e) {
						LogUtil.error("Auto save to file fail, fi={}, f={}", e, fi.getFieldName(), fi.getName());
					}
				}
			}

			return ups;
		} catch (FileUploadException e) {
			LogUtil.error("parse upload fail", e);
		}

		return ups;
	}

	private static ServletFileUpload initUpload(String encoding) {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		String tmpdir = Args.get("upload.tmpdir");
		if (tmpdir != null) {
			factory.setRepository(new File(tmpdir));
		}

		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setHeaderEncoding(encoding);
		return upload;
	}
}
