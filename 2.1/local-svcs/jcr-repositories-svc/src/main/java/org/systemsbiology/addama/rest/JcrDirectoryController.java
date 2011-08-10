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
package org.systemsbiology.addama.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.rest.json.NodeDirectoryJSONObject;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

/**
 * @author hrovira
 */
@Controller
public class JcrDirectoryController extends AbstractJcrController {

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView directory(HttpServletRequest request) throws Exception {
        log.info("directory");

        Node node = getNode(request, "/dir");

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", new NodeDirectoryJSONObject(node, request, dateFormat));
        return mav;
    }

}