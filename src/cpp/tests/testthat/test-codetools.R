#
# test-codetools.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

library(testthat)

context("codetools")

test_that(".rs.detectFreeVars() works as expected", {
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         paste(apple + banana)
      }),
      
      c("apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         base::paste(apple + banana)
      }),
      
      c("apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         userDefinedFunction(apple, banana)
      }),
      
      c("userDefinedFunction", "apple", "banana")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         for (i in 1:10) {
            print(i + j)
         }
      }),
      
      c("j")
      
   )
   
   expect_equal(
      
      .rs.detectFreeVarsExpr({
         udf <- function(apple, banana) {
            apple + banana + cherry + danish
         }
      }),
      
      c("cherry", "danish")
      
   )
   
   
})

test_that(".rs.CRANDownloadOptionsString() generates a valid R expression", {
   
   # restore options when done
   op <- options()
   on.exit(options(op), add = TRUE)
   
   # set up options used by download string
   options(
      repos = c(CRAN = "https://cran.rstudio.com"),
      download.file.method = "libcurl",
      download.file.extra = NULL,
      HTTPUserAgent = "dummy"
   )

   # create a dummy environment that makes it easier for us
   # to 'capture' the result of an options call
   envir <- new.env(parent = globalenv())
   envir[["options"]] <- base::list
   
   # check that we construct the right kind of R object after parse
   #
   # NOTE: we don't depend on the exact representation of the string as the
   # code returned by R's deparser might differ from version to version,
   # but should still produce the same result after evaluation
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   for (item in c("repos", "download.file.method", "HTTPUserAgent"))
      expect_equal(actual[[item]], getOption(item))
   
   # https://github.com/rstudio/rstudio/issues/6597
   options(download.file.extra = "embedded 'quotes'")
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(actual$download.file.extra, "embedded 'quotes'")
   
   # NOTE: double-quotes are translated to single-quotes here as
   # a workaround for issues with quotation of arguments when running
   # commands on Windows
   options(download.file.extra = "embedded \"quotes\"")
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(actual$download.file.extra, "embedded 'quotes'")
   
})

test_that(".rs.CRANDownloadOptionsString() fills missing CRAN repo", {
   # read default CRAN URL from user preferences
   cran <- .rs.readUiPref("cran_mirror")$url

   # unset repos
   options(repos = NULL)

   # create dummy environment
   envir <- new.env(parent = globalenv())
   envir[["options"]] <- base::list

   # evaluate the download string and confirm that it contains the CRAN URL from user prefs
   string <- .rs.CRANDownloadOptionsString()
   actual <- eval(parse(text = string), envir = envir)
   expect_equal(unlist(actual[["repos"]]["CRAN"]), c(CRAN = cran))
})


test_that("HTML escaping escapes HTML entities", {
   fake_header <- "<h1>Not a real header.</h1>"

   escaped <- .rs.htmlEscape(fake_header)
   expect_false(grepl(escaped, "<"))
   expect_false(grepl(escaped, ">"))
})

test_that("heredoc trims trailing newlines + whitespace", {
   
   actual <- .rs.heredoc('
      c(
         person(
            "Jane", "Doe",
            email = "jane@example.com",
            role = c("aut", "cre")
         )
      )
   ')
   
   expected <- 'c(
   person(
      "Jane", "Doe",
      email = "jane@example.com",
      role = c("aut", "cre")
   )
)'
   
   expect_equal(actual, expected)
   
})
