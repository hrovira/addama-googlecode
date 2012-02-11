/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.appengine.pojos;

import com.google.appengine.api.urlfetch.HTTPResponse;

import java.util.concurrent.Future;

/**
 * @author hrovira
 */
public class SearchableResponse {
    private final Future<HTTPResponse> futureResponse;
    private final RegistryService searchable;

    public SearchableResponse(Future<HTTPResponse> futureResponse, RegistryService registryService) {
        this.futureResponse = futureResponse;
        this.searchable = registryService;
    }

    public Future<HTTPResponse> getFutureResponse() {
        return futureResponse;
    }

    public RegistryService getSearchable() {
        return searchable;
    }
}
