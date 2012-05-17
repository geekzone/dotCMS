/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.ejb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.ImageUtil;

/**
 * 
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class CompanyManagerImpl
	extends PrincipalBean implements CompanyManager {

	// Business methods

	public Company getCompany() throws PortalException, SystemException {
		Company company =
			CompanyUtil.findByPrimaryKey(CompanyUtils.getDefaultCompany().getCompanyId());

		return (Company)company.getProtected();
	}

	public Company getCompany(String companyId)
		throws PortalException, SystemException {

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		return (Company)company.getProtected();
	}

	public Company updateCompany(
			String portalURL, String homeURL, String mx, String name,
			String shortName, String type, String size, String street,
			String city, String state, String zip, String phone, String fax,
			String emailAddress, String authType, boolean autoLogin,
			boolean strangers)
		throws PortalException, SystemException {

		Company company =
			CompanyUtil.findByPrimaryKey(CompanyUtils.getDefaultCompany().getCompanyId());

		if (!hasAdministrator(company.getCompanyId())) {
			throw new PrincipalException();
		}

		company.setPortalURL(portalURL);
		company.setHomeURL(homeURL);

		if (GetterUtil.get(
				PropsUtil.get(PropsUtil.MAIL_MX_UPDATE), true)) {

			company.setMx(mx);
		}

		company.setName(name);
		company.setShortName(shortName);
		company.setType(type);
		company.setSize(size);
		company.setStreet(street);
		company.setCity(city);
		company.setState(state);
		company.setZip(zip);
		company.setPhone(phone);
		company.setFax(fax);
		company.setEmailAddress(emailAddress);
		company.setAuthType(authType);
		company.setAutoLogin(autoLogin);
		company.setStrangers(strangers);

		CompanyUtil.update(company);

		return company;
	}

	public Company updateCompany(Company company) throws SystemException {
		return CompanyUtil.update(company);
	}
	
	public void updateLogo(File file) throws PortalException, SystemException {
		String companyId = CompanyUtils.getDefaultCompany().getCompanyId();

		if (!hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		try {
			ImageLocalUtil.put(companyId, FileUtil.getBytes(file));

			BufferedImage thumbnail = ImageUtil.scale(ImageIO.read(file), .6);

			// PNG

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			ImageIO.write(thumbnail, "png", baos);

			ImageLocalUtil.put(companyId + ".png", baos.toByteArray());

			// WBMP

			baos = new ByteArrayOutputStream();

			ImageUtil.encodeWBMP(thumbnail, baos);

			ImageLocalUtil.put(companyId + ".wbmp", baos.toByteArray());
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}
	
	public void updateUser(
			String languageId, String timeZoneId, String skinId,
			boolean dottedSkins, boolean roundedSkins, String resolution, User user)
		throws PortalException, SystemException {

		Company company =
			CompanyUtil.findByPrimaryKey(CompanyUtils.getDefaultCompany().getCompanyId());

		if (!hasAdministrator(company.getCompanyId())) {
			throw new PrincipalException();
		}

		try {
			APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), true);
		} catch (Exception e) {
			Logger.error(CompanyManagerImpl.class,e.getMessage(),e);
			throw new SystemException(e);
		}
	}
	
	
	
	public void updateUsers(String languageId, String timeZoneId, String skinId,
			boolean dottedSkins, boolean roundedSkins, String resolution)
		throws PortalException, SystemException,com.dotmarketing.exception.DotRuntimeException {

		String ds  = DbConnectionFactory.getDBFalse();
		String rs = DbConnectionFactory.getDBFalse();
		
		if(dottedSkins)
			ds = DbConnectionFactory.getDBTrue();
		if(roundedSkins)
			rs = DbConnectionFactory.getDBTrue();
		
		Company company =
			CompanyUtil.findByPrimaryKey(CompanyUtils.getDefaultCompany().getCompanyId());
	     try{
			DotConnect dc = new DotConnect();
			dc.setSQL("update user_ set languageid = ?, timezoneid = ?, skinid = ?, dottedskins = " + rs + ", roundedskins = " + ds + ", resolution = ? where companyid = ?");
			dc.addParam(languageId);
			dc.addParam(timeZoneId);
			dc.addParam(skinId);
			dc.addParam(resolution);
			dc.addParam(company.getCompanyId());
			dc.getResult();
		}catch (Exception e) {
			throw new DotRuntimeException(e.getMessage());
		}
	
	}

	
}