package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.download.Downloader;
import com.muwire.core.download.UIDownloadCancelledEvent;
import com.muwire.core.download.UIDownloadEvent;
import com.muwire.core.search.UIResultEvent;

import net.i2p.data.Base64;

public class DownloadServlet extends HttpServlet {

    private DownloadManager downloadManager;
    private SearchManager searchManager;
    private Core core;
    
    public void init(ServletConfig config) throws ServletException {
        downloadManager = (DownloadManager) config.getServletContext().getAttribute("downloadManager");
        searchManager = (SearchManager) config.getServletContext().getAttribute("searchManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String infoHashB64 = req.getParameter("infoHash");
        InfoHash infoHash = new InfoHash(Base64.decode(infoHashB64));
        String action = req.getParameter("action");
        if (action.equals("start")) {
            UUID uuid = UUID.fromString(req.getParameter("uuid"));
            Set<UIResultEvent> results = searchManager.getResults().get(uuid).getByInfoHash(infoHash);
            
            UIDownloadEvent event = new UIDownloadEvent();
            UIResultEvent[] resultsArray = results.toArray(new UIResultEvent[0]);
            event.setResult(resultsArray);
            // TODO: sequential
            // TODO: possible sources
            event.setSources(Collections.emptySet());
            event.setTarget(new File(core.getMuOptions().getDownloadLocation(), resultsArray[0].getName()));
            core.getEventBus().publish(event);
        } else if (action.equals("cancel")) {
            downloadManager.getDownloaders().stream().filter(d -> d.getInfoHash().equals(infoHash)).findAny().
                    ifPresent(d -> {
                        d.cancel();
                        downloadManager.getDownloaders().remove(d);
                        UIDownloadCancelledEvent event = new UIDownloadCancelledEvent();
                        event.setDownloader(d);
                        core.getEventBus().publish(event);
                    });
        }
        resp.sendRedirect("/MuWire/Downloads.jsp");
    }
}