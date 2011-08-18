/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.rest.exceptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author waltz
 */
public class DataOneExceptionViewResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object object, Exception exception) {
        BaseException baseException;
        if (!(exception instanceof BaseException)){
            baseException = new ServiceFailure("100", exception.getMessage());
        } else {
            baseException = (BaseException) exception;
        }

        ModelAndView mav = new ModelAndView("xmlBaseExceptionViewResolver", "org.dataone.service.exception.BaseException", baseException);
        return mav;
    }

}
