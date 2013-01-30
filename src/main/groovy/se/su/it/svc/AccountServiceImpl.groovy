package se.su.it.svc

import javax.jws.WebService
import org.apache.log4j.Logger
import se.su.it.svc.commons.SvcAudit
import javax.jws.WebParam
import se.su.it.svc.manager.GldapoManager
import se.su.it.svc.query.SuPersonQuery
import se.su.it.svc.ldap.SuPerson
import se.su.it.commons.Kadmin
import se.su.it.commons.PasswordUtils
import se.su.it.svc.audit.AuditAspectMethodDetails
import se.su.it.svc.commons.SvcSuPersonVO
import se.su.it.svc.ldap.SuRole
import se.su.it.svc.query.SuRoleQuery
import org.springframework.ldap.core.DistinguishedName
import se.su.it.svc.ldap.SuInitPerson
import se.su.it.svc.util.AccountServiceUtils
import java.util.regex.Pattern
import java.util.regex.Matcher
import se.su.it.commons.ExecUtils

/**
 * Implementing class for AccountService CXF Web Service.
 * This Class handles all Account activities in SUKAT.
 */

@WebService
public class AccountServiceImpl implements AccountService{
  private static final Logger logger = Logger.getLogger(AccountServiceImpl.class)

  /**
   * This method sets the primary affiliation for the specified uid.
   *
   *
   * @param uid  uid of the user.
   * @param affiliation the affiliation for this uid
   * @param audit Audit object initilized with audit data about the client and user.
   * @return array of SuService.
   * @see se.su.it.svc.ldap.SuPerson
   * @see se.su.it.svc.commons.SvcAudit
   */
  public void updatePrimaryAffiliation(@WebParam(name = "uid") String uid, @WebParam(name = "affiliation") String affiliation, @WebParam(name = "audit") SvcAudit audit) {
    if(uid == null || affiliation == null || audit == null)
      throw new java.lang.IllegalArgumentException("updatePrimaryAffiliation - Null argument values not allowed in this function")
    SuPerson person = SuPersonQuery.getSuPersonFromUID(GldapoManager.LDAP_RW, uid)
    if(person) {
      logger.debug("updatePrimaryAffiliation - Replacing affiliation=<${person?.eduPersonPrimaryAffiliation}> with affiliation=<${affiliation}> for uid=<${uid}>")
      person.eduPersonPrimaryAffiliation = affiliation
      SuPersonQuery.saveSuPerson(person)
      logger.info("updatePrimaryAffiliation - Updated affiliation for uid=<${uid}> with affiliation=<${person.eduPersonPrimaryAffiliation}>")
    } else {
      throw new IllegalArgumentException("updatePrimaryAffiliation no such uid found: "+uid)
    }
  }

  /**
   * This method resets the password for the specified uid and returns the clear text password.
   *
   *
   * @param uid  uid of the user.
   * @param audit Audit object initilized with audit data about the client and user.
   * @return String new password.
   * @see se.su.it.svc.commons.SvcAudit
   */
  public String resetPassword(@WebParam(name = "uid") String uid, @WebParam(name = "audit") SvcAudit audit) {
    if (uid == null || audit == null)
      throw new java.lang.IllegalArgumentException("resetPassword - Null argument values not allowed in this function")
    String trueUid = uid.replaceFirst("\\.", "/")
    if (Kadmin.newInstance().principalExists(trueUid)) {
      logger.debug("resetPassword - Trying to reset password for uid=<${uid}>")
      String pwd = PasswordUtils.genRandomPassword(10, 10)
      Kadmin.newInstance().setPassword(trueUid, pwd)
      logger.info("resetPassword - Password was reset for uid=<${uid}>")
      return pwd
    } else {
      logger.debug("resetPassword - No such uid found: "+uid)
      throw new java.lang.IllegalArgumentException("resetPassword - No such uid found: "+uid)
    }
    return null
  }

  /**
   * This method updates the attributes for the specified uid.
   *
   * @param uid  uid of the user.
   * @param role String with DN that points to an SuRole or null.
   * @param person pre-populated SvcSuPersonVO object, the attributes that differ in this object to the original will be updated in ldap.
   * @param audit Audit object initilized with audit data about the client and user.
   * @return void.
   * @see se.su.it.svc.ldap.SuPerson
   * @see se.su.it.svc.commons.SvcAudit
   */
  public void updateSuPerson(@WebParam(name = "uid") String uid, @WebParam(name = "roleDN") String roleDN, @WebParam(name = "person") SvcSuPersonVO person, @WebParam(name = "audit") SvcAudit audit){
    if (uid == null || person == null || audit == null)
      throw new java.lang.IllegalArgumentException("updateSuPerson - Null argument values not allowed for uid, person or audit")

    SuPerson originalPerson = SuPersonQuery.getSuPersonFromUID(GldapoManager.LDAP_RW, uid)
    if(originalPerson) {
      originalPerson.applySuPersonDifference(person)
      logger.debug("updateSuPerson - Trying to update SuPerson uid<${originalPerson.uid}>")
      SuPersonQuery.saveSuPerson(originalPerson)
      logger.info("updateSuPerson - Updated SuPerson uid<${originalPerson.uid}>")
      if(roleDN != null) {
        logger.debug("updateSuPerson - Trying to find role for DN<${roleDN}>")
        SuRole role = SuRoleQuery.getSuRoleFromDN(GldapoManager.LDAP_RW, roleDN)
        if(role != null) {
          logger.debug("updateSuPerson - Role <${role.cn}> found for DN<${roleDN}>")
          DistinguishedName uidDN = new DistinguishedName(originalPerson.getDn())
          def roList = role.roleOccupant.collect { ro -> new DistinguishedName(ro) }
          if(!roList.contains(uidDN)) {
            role.roleOccupant.add(uidDN.toString())
            SuRoleQuery.saveSuRole(role)
            logger.info("updateSuPerson - Uid<${originalPerson.uid}> added as occupant to role <${role.cn}> ")
          } else {
            logger.debug("updateSuPerson - Occupant <${originalPerson.uid}> already exist for role <${role.cn}>")
          }
        } else {
          logger.warn("updateSuPerson - Could not update uid <${originalPerson.uid}> with role <${roleDN}>, role not found!")
        }
      }
    } else {
      throw new IllegalArgumentException("updateSuPerson - No such uid found: "+uid)
    }
  }

  public String createSuPerson(@WebParam(name = "uid") String uid, @WebParam(name = "domain") String domain, @WebParam(name = "nin") String nin, @WebParam(name = "givenName") String givenName, @WebParam(name = "sn") String sn, @WebParam(name = "roleDN") String roleDN, @WebParam(name = "person") SvcSuPersonVO person, @WebParam(name = "audit") SvcAudit audit) {
    if (uid == null || domain == null || nin == null || givenName == null || sn == null || person == null || audit == null)
      throw new java.lang.IllegalArgumentException("createSuPerson - Null argument values not allowed for uid, domain, nin, givenName, sn, person or audit")
    if(SuPersonQuery.getSuPersonFromUID(GldapoManager.LDAP_RO, uid))
      throw new java.lang.IllegalArgumentException("createSuPerson - A user with uid <"+uid+"> already exists")

    //Begin init entry in sukat
    SuInitPerson suInitPerson = new SuInitPerson()
    suInitPerson.uid = uid
    suInitPerson.cn = givenName + " " + sn
    suInitPerson.sn = sn
    suInitPerson.givenName = givenName
    suInitPerson.norEduPersonNIN = nin
    suInitPerson.eduPersonPrincipalName = uid + "@su.se"
    suInitPerson.objectClass = ["suPerson","sSNObject","norEduPerson","eduPerson","inetOrgPerson","organizationalPerson","person","top"]
    suInitPerson.parent = AccountServiceUtils.domainToDN(domain)
    SuPersonQuery.initSuPerson(GldapoManager.LDAP_RW, suInitPerson)
    //End init entry in sukat

    //Begin call Perlscript to init user in kdc, afs and unixshell
    //Maybe we want to replace this with a call to the message bus in the future
    boolean error = false
    String uidNumber = ""
    String output = ""
    String password = PasswordUtils.genRandomPassword(10, 10);
    def perlScript = ["--user", "uadminw", "/local/sukat/libexec/enable-user.pl", "--uid", uid, "--password", password, "--gidnumber", "1200"]
    try {
      def res = ExecUtils.exec("/local/scriptbox/bin/run-token-script.sh", perlScript.toArray(new String[perlScript.size()]))
      Pattern p = Pattern.compile("OK \\(uidnumber:(\\d+)\\)")
      Matcher m = p.matcher(res.trim())
      if (m.matches()) {
        uidNumber = m.group(1)
      } else {
        error = true
      }
    } catch (Exception e) {
      error = true;
      logger.error("createSuPerson - Error when creating uid<${uid}> in KDC and/or AFS! Error: " + e.message)
      logger.error("               - posixAccount attributes will not be written to SUKAT!")
    }
    //End call Perlscript to init user in kdc, afs and unixshell
    if(!error) {
      suInitPerson.objectClass.add("posixAccount")
      suInitPerson.loginShell = "/usr/local/bin/bash"
      suInitPerson.homeDirectory = "/afs/su.se/home/"+uid.charAt(0)+"/"+uid.charAt(1)+"/"+uid
      suInitPerson.uidNumber = uidNumber
      suInitPerson.gidNumber = "1200"

      SuPersonQuery.saveSuInitPerson(suInitPerson)
    }
    updateSuPerson(uid,roleDN,person,audit)
    return password
  }

}
